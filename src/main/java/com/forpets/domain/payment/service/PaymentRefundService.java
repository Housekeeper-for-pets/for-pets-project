package com.forpets.domain.payment.service;

import com.forpets.domain.coupon.service.CouponService;
import com.forpets.domain.payment.client.PortOneCancelResult;
import com.forpets.domain.payment.client.PortOnePaymentClient;
import com.forpets.domain.payment.entity.Payment;
import com.forpets.domain.payment.entity.PaymentRole;
import com.forpets.domain.payment.entity.PaymentStatus;
import com.forpets.domain.payment.exception.PaymentErrorCode;
import com.forpets.domain.payment.exception.PaymentException;
import com.forpets.domain.payment.repository.PaymentRepository;
import com.forpets.domain.reservation.entity.CanceledBy;
import com.forpets.domain.reservation.entity.ReservationPayment;
import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import com.forpets.domain.reservation.repository.ReservationPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentRefundService {

    private final PaymentRepository paymentRepository;
    private final ReservationPaymentRepository reservationPaymentRepository;
    private final PortOnePaymentClient portOnePaymentClient;
    private final CouponService couponService;
    private final PaymentLockService paymentLockService;


    private static final int PENALTY_PERCENT = 20;

    @Transactional
    public void syncRefundedByWebhook(String merchantUid, String reason, String rawResponse) {
        Payment payment = paymentRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        paymentLockService.executeWithReservationLock(payment.getReservationId(), () -> {
            if (!payment.isPaid()) {
                return null;
            }

            ReservationPayment reservationPayment = reservationPaymentRepository
                    .findByReservationId(payment.getReservationId())
                    .orElseThrow(() -> new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));

            payment.refund(reason, rawResponse);
            restoreReservationPayment(payment, reservationPayment);
            restoreCouponIfUsed(payment);
            return null;
        });
    }

    @Transactional
    public void refundPaidPayments(Long reservationId, String reason) {
        paymentLockService.executeWithReservationLock(reservationId, () -> {
            List<Payment> paidPayments = paymentRepository.findAllByReservationIdAndStatusIn(
                    reservationId, List.of(PaymentStatus.PAID));

            if (paidPayments.isEmpty()) {
                return null;
            }

            ReservationPayment reservationPayment = reservationPaymentRepository.findByReservationId(reservationId)
                    .orElseThrow(() -> new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));

            paidPayments.forEach(payment -> refund(payment, reservationPayment, reason));
            return null;
        });
    }

    private void refund(Payment payment, ReservationPayment reservationPayment, String reason) {
        if (!payment.isPaid()) {
            return;
        }

        /*
        외부 PG 환불이 먼저 성공해야 로컬 결제 상태를 REFUNDED로 닫는다.
        이 순서를 지키면 PortOne 환불 실패 시 내부 상태만 먼저 바뀌는 상황을 막을 수 있다.
         */
        PortOneCancelResult cancelResult = portOnePaymentClient.cancelPayment(
                payment.getMerchantUid(), payment.getFinalAmount(), reason);

        payment.refund(reason, cancelResult.rawResponse());
        restoreReservationPayment(payment, reservationPayment);
        restoreCouponIfUsed(payment);

        log.info("[PaymentRefundService] 결제 환불 완료 paymentId={}, reservationId={}, role={}",
                payment.getId(), payment.getReservationId(), payment.getPaymentRole());
    }

    private void restoreReservationPayment(Payment payment, ReservationPayment reservationPayment) {
        if (payment.getPaymentRole() == PaymentRole.GUARDIAN) {
            reservationPayment.guardianRefund();
            return;
        }
        reservationPayment.sitterRefund();
    }

    private void restoreCouponIfUsed(Payment payment) {
        if (payment.getUserCouponId() != null) {
            couponService.restoreCoupon(payment.getMemberId(), payment.getUserCouponId());
        }
    }

    /*
     * 창을 열기만 하고 결제하진 않은 건(READY/PENDING)을  EXPIRED 처리
     * refundPaidPayments()랑 같이 호출하기
     */
    @Transactional
    public void expireNonPaidPayments(Long reservationId) {
        List<Payment> nonPaidPayments = paymentRepository.findAllByReservationIdAndStatusIn(
                reservationId, List.of(PaymentStatus.READY, PaymentStatus.PENDING));

        nonPaidPayments.forEach(payment -> {
            payment.expire();
            restoreCouponIfUsed(payment);
            log.info("[PaymentRefundService] 미결제 Payment 만료 처리 paymentId={}, reservationId={}, 기존상태={}",
                    payment.getId(), payment.getReservationId(), payment.getStatus());
        });
    }


    @Transactional
    public void refundWithPenalty(Long reservationId, String reason, CanceledBy canceledBy) {
        paymentLockService.executeWithReservationLock(reservationId, () -> {
            List<Payment> paidPayments = paymentRepository.findAllByReservationIdAndStatusIn(
                    reservationId, List.of(PaymentStatus.PAID));

            if (paidPayments.isEmpty()) {
                return null;
            }

            ReservationPayment reservationPayment = reservationPaymentRepository
                    .findByReservationId(reservationId)
                    .orElseThrow(() -> new ReservationException(
                            ReservationErrorCode.RESERVATION_NOT_FOUND));

            for (Payment payment : paidPayments) {
                if (!payment.isPaid()) continue;

                if (shouldApplyPenalty(payment, canceledBy)) {
                    // 위약금 대상: 80%만 환불
                    long refundAmount = payment.getFinalAmount() * (100 - PENALTY_PERCENT) / 100;
                    partialRefund(payment, reservationPayment, refundAmount, reason);
                } else {
                    // 위약금 비대상: 전액 환불
                    refund(payment, reservationPayment, reason);
                }
            }
            return null;
        });
    }


    /**
     * 위약금 적용 대상 판별
     * - 보호자가 취소 → 보호자 결제가 위약금 대상 (80%만 환불)
     * - 시터가 취소 → 시터 예약금이 위약금 대상 (80%만 환불, 20%는 보호자에게)
     */
    private boolean shouldApplyPenalty(Payment payment, CanceledBy canceledBy) {
        if (canceledBy == CanceledBy.GUARDIAN) {
            return payment.getPaymentRole() == PaymentRole.GUARDIAN;
        }
        return payment.getPaymentRole() == PaymentRole.SITTER;
    }

    private void partialRefund(Payment payment, ReservationPayment reservationPayment,
                               Long refundAmount, String reason) {
        PortOneCancelResult cancelResult = portOnePaymentClient.cancelPayment(
                payment.getMerchantUid(), refundAmount, reason);

        payment.refund(reason, cancelResult.rawResponse());
        restoreReservationPayment(payment, reservationPayment);
        restoreCouponIfUsed(payment);

        log.info("[PaymentRefundService] 위약금 차감 환불 완료 paymentId={}, 원래금액={}, 환불금액={}, 위약금={}",
                payment.getId(), payment.getFinalAmount(), refundAmount,
                payment.getFinalAmount() - refundAmount);
    }
}
