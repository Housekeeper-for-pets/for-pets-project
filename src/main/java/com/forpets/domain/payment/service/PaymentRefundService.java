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

    /*
    merchantUid로 Payment 조회, Lock 걸기
    - PAID 가 아니면 이미 환불되었거나 다른상태니까 early return
    - PAID 상태면 환불 처리
        PaymentStauts -> REFUNDED
        ReservationPayment -> 다시 paid = false 처리
        쿠폰을 썼다면 복구
     */
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

    /*
    반복문 돌려서 이미 지불된 상태인 Payment List 찾고 refund method 일괄 적용
     */
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

    /*
    - Payment status = REFUNDED, reason 저장, rawResponse 저장, refundedAt = now()
    - ReservationPayment table 에도 true로 되어있는거 false 로 다시 바꿔주기 (restoreReservationPayment)
    - Payment table 에 UserCouponId 가 저장되어있다면 restore 해주기
     */
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

    // reservation payment 에 paid = true 처리 되어있는거 false 로 다시 바꿔주기
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
}
