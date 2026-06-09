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
import com.forpets.domain.reservation.service.ReservationLockService;
import com.forpets.domain.settlement.entity.SettlementType;
import com.forpets.domain.settlement.service.SettlementService;
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
    private final ReservationLockService reservationLockService;
    private final SettlementService settlementService;


    private static final int PENALTY_PERCENT = 20;

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

        reservationLockService.executeWithReservationLock(payment.getReservationId(), () -> {
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

    [정합성 안전장치]
    PAID 상태인 Payment row 가 없는데 ReservationPayment 의 guardianPaid/sitterPaid 플래그가 true 인
    "stale paid 플래그" 상태가 과거에 관측된 적이 있다. (reservation EXPIRED 처리됐지만 payment 는 PAID 로 stuck)
    이 경우 단순 early return 하면 ReservationPayment 가 영원히 paid=true 로 남아 만료/환불 표시가 안 됨.
    → PAID 결제건이 없어도 ReservationPayment 의 paid 플래그가 켜져 있으면 정합성 깨진 상태로 간주하고
      WARN 로그 + 강제로 false 동기화한다.
     */
    @Transactional
    public void refundPaidPayments(Long reservationId, String reason) {
        List<Payment> paidPayments = paymentRepository.findAllByReservationIdAndStatusIn(
                reservationId, List.of(PaymentStatus.PAID));

        ReservationPayment reservationPayment = reservationPaymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        if (paidPayments.isEmpty()) {
            forceSyncReservationPaymentIfDirty(reservationId, reservationPayment);
            return;
        }

        paidPayments.forEach(payment -> refund(payment, reservationPayment, reason));
    }

    /*
    Payment 테이블엔 PAID row 가 없는데 ReservationPayment 에는 paid=true 로 남아있는 stale 상태 감지 + 정정.
    운영에서 이 WARN 로그가 찍히면 데이터 정합성 깨진 케이스가 있는지 추적해야 함.
     */
    private void forceSyncReservationPaymentIfDirty(Long reservationId, ReservationPayment reservationPayment) {
        if (reservationPayment.isGuardianPaid()) {
            log.warn("[PaymentRefundService][정합성 정정] PAID Payment 없는데 guardianPaid=true → 강제 false 동기화 reservationId={}",
                    reservationId);
            reservationPayment.guardianRefund();
        }
        if (reservationPayment.isSitterPaid()) {
            log.warn("[PaymentRefundService][정합성 정정] PAID Payment 없는데 sitterPaid=true → 강제 false 동기화 reservationId={}",
                    reservationId);
            reservationPayment.sitterRefund();
        }
    }

    /*
    정상 케어 완료 후 시터가 냈던 예약금만 환불한다.
    보호자 결제금은 시터 정산 Settlement 의 원천이므로 여기서 환불하지 않는다.
    호출자는 reservationId 기준 Lock 을 잡은 상태에서 호출해야 한다.
     */
    @Transactional
    public void refundSitterDepositAfterCompletion(Long reservationId) {
        Payment sitterPayment = paymentRepository.findByReservationIdAndPaymentRoleAndStatus(
                        reservationId, PaymentRole.SITTER, PaymentStatus.PAID)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        ReservationPayment reservationPayment = reservationPaymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        refund(sitterPayment, reservationPayment, "케어 완료에 따른 시터 예약금 환불");
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
     * 예약 만료(Expire) 흐름에서 호출 — 시간 초과로 무효화되는 케이스
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

    /*
     * 창을 열기만 하고 결제하진 않은 건(READY/PENDING)을 CANCELED 처리
     * 예약 취소(Cancel) 흐름에서 호출 — 사용자 의도로 무효화되는 케이스
     * EXPIRED 와 의미상 분리해서 통계/추적 시 혼동을 방지한다
     */
    @Transactional
    public void cancelNonPaidPayments(Long reservationId, String reason) {
        List<Payment> nonPaidPayments = paymentRepository.findAllByReservationIdAndStatusIn(
                reservationId, List.of(PaymentStatus.READY, PaymentStatus.PENDING));

        nonPaidPayments.forEach(payment -> {
            payment.cancel(reason, null);
            restoreCouponIfUsed(payment);
            log.info("[PaymentRefundService] 미결제 Payment 취소 처리 paymentId={}, reservationId={}, 사유={}",
                    payment.getId(), payment.getReservationId(), reason);
        });
    }


    @Transactional
    public void refundWithPenalty(Long reservationId, String reason, CanceledBy canceledBy) {
        List<Payment> paidPayments = paymentRepository.findAllByReservationIdAndStatusIn(
                reservationId, List.of(PaymentStatus.PAID));

        if (paidPayments.isEmpty()) {
            return;
        }

        ReservationPayment reservationPayment = reservationPaymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        for (Payment payment : paidPayments) {
            if (!payment.isPaid()) continue;

            long penalty = calculatePenalty(payment, canceledBy);
            long refundAmount = payment.getFinalAmount() - penalty;

            /*
            [보호자 일방 취소 + 시터 예약금]
            시터에게는 귀책 사유가 없으므로 즉시 PG 전액 환불해서 묶어두지 않는다.
            (보호자 결제분은 Settlement 로 관리자 수동 처리해야 해서 시간이 걸리지만
             시터 예약금은 즉시 풀어줘야 사용자 경험상 자연스러움)
            Settlement 도 생성하지 않으므로 continue 로 다음 payment 처리.
             */
            if (canceledBy == CanceledBy.GUARDIAN
                    && payment.getPaymentRole() == PaymentRole.SITTER) {
                refund(payment, reservationPayment, "보호자 취소에 따른 시터 예약금 환불");
                continue;
            }

            // PG 호출 없이 상태만 REFUNDED로 닫음 (부분 취소 PG 제약 우회)
            markAsRefundedWithoutPgCall(payment, reservationPayment, reason);

            // 보호자 취소: 환불분 Settlement + 위약금 Settlement 각각 생성
            // 시터 취소: 보호자 전액 환불은 기존 refund() 호출, 시터 쪽만 이 분기 탐
            if (canceledBy == CanceledBy.GUARDIAN
                    && payment.getPaymentRole() == PaymentRole.GUARDIAN) {

                if (refundAmount > 0) {
                    settlementService.createGuardianRefundSettlement(
                            reservationId,
                            payment.getMemberId(),
                            payment.getId(),
                            refundAmount,
                            "보호자 취소 환불분 - " + reason
                    );
                }

                if (penalty > 0) {
                    Long sitterMemberId = findPenaltyReceiverMemberId(paidPayments, canceledBy);
                    settlementService.createPenaltySettlement(
                            reservationId,
                            sitterMemberId,
                            payment.getId(),
                            penalty,
                            SettlementType.OWNER_CANCEL_PENALTY,
                            getPenaltySettlementReason(canceledBy, reason)
                    );
                }
            }

            // 시터 취소: 시터 위약금 Settlement (보호자 결제는 별도 refund() 로 PG 전액 환불)
            if (canceledBy == CanceledBy.SITTER
                    && payment.getPaymentRole() == PaymentRole.SITTER
                    && penalty > 0) {

                Long guardianMemberId = findPenaltyReceiverMemberId(paidPayments, canceledBy);
                settlementService.createPenaltySettlement(
                        reservationId,
                        guardianMemberId,
                        payment.getId(),
                        penalty,
                        SettlementType.SITTER_CANCEL_PENALTY,
                        getPenaltySettlementReason(canceledBy, reason)
                );
            }
        }
    }

    /*
     * 위약금 계산
     * - 보호자가 취소: 보호자가 자기 결제금의 20% 위약금
     * - 시터가 취소: 시터가 자기 예약금 100% 위약금
     * - 취소 안 한 쪽: 위약금 0 (전액 환불)
     */
    private long calculatePenalty(Payment payment, CanceledBy canceledBy) {
        if (canceledBy == CanceledBy.GUARDIAN
                && payment.getPaymentRole() == PaymentRole.GUARDIAN) {
            return payment.getFinalAmount() * PENALTY_PERCENT / 100;
        }
        if (canceledBy == CanceledBy.SITTER
                && payment.getPaymentRole() == PaymentRole.SITTER) {
            return payment.getFinalAmount(); // 시터 예약금 전액
        }
        return 0L;
    }

    private Long findPenaltyReceiverMemberId(List<Payment> paidPayments, CanceledBy canceledBy) {
        PaymentRole receiverRole = canceledBy == CanceledBy.GUARDIAN
                ? PaymentRole.SITTER
                : PaymentRole.GUARDIAN;

        return paidPayments.stream()
                .filter(payment -> payment.getPaymentRole() == receiverRole)
                .findFirst()
                .map(Payment::getMemberId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    private SettlementType getPenaltySettlementType(CanceledBy canceledBy) {
        if (canceledBy == CanceledBy.GUARDIAN) {
            return SettlementType.OWNER_CANCEL_PENALTY;
        }
        return SettlementType.SITTER_CANCEL_PENALTY;
    }

    private String getPenaltySettlementReason(CanceledBy canceledBy, String cancelReason) {
        String prefix = canceledBy == CanceledBy.GUARDIAN
                ? "보호자 귀책 취소 보상"
                : "시터 귀책 취소 보상";
        return prefix + " - " + cancelReason;
    }
    private void partialRefund(Payment payment, ReservationPayment reservationPayment,
                               Long refundAmount, String reason) {
        PortOneCancelResult cancelResult = portOnePaymentClient.cancelPayment(
                payment.getMerchantUid(), refundAmount, reason);

        payment.refund(reason, cancelResult.rawResponse());
        restoreReservationPayment(payment, reservationPayment);
        restoreCouponIfUsed(payment);

        log.info("[PaymentRefundService] 부분 환불 완료 paymentId={}, 원래금액={}, 환불금액={}, 위약금={}",
                payment.getId(), payment.getFinalAmount(), refundAmount,
                payment.getFinalAmount() - refundAmount);
    }


    private void markAsRefundedWithoutPgCall(Payment payment,
                                             ReservationPayment reservationPayment,
                                             String reason) {
        payment.refund(reason, null);
        restoreReservationPayment(payment, reservationPayment);
        restoreCouponIfUsed(payment);

        log.info("[PaymentRefundService] PG 환불 없이 상태만 닫음 paymentId={}, 전액위약금={}",
                payment.getId(), payment.getFinalAmount());
    }
}
