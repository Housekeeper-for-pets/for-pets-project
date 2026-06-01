package com.forpets.domain.payment.service;

import com.forpets.domain.coupon.dto.CouponApplyResult;
import com.forpets.domain.coupon.service.CouponService;
import com.forpets.domain.payment.client.PortOnePaymentClient;
import com.forpets.domain.payment.client.PortOnePaymentResult;
import com.forpets.domain.payment.dto.ConfirmPaymentRequest;
import com.forpets.domain.payment.dto.ConfirmPaymentResponse;
import com.forpets.domain.payment.dto.CreatePaymentRequest;
import com.forpets.domain.payment.dto.FailPaymentRequest;
import com.forpets.domain.payment.dto.PaymentResponseDto;
import com.forpets.domain.payment.entity.*;
import com.forpets.domain.payment.exception.PaymentErrorCode;
import com.forpets.domain.payment.exception.PaymentException;
import com.forpets.domain.payment.repository.PaymentRepository;
import com.forpets.domain.reservation.dto.ReservationResponseDto;
import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationPayment;
import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import com.forpets.domain.reservation.repository.ReservationPaymentRepository;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.domain.reservation.service.ReservationLockService;
import com.forpets.domain.reservation.service.ReservationService;
import com.forpets.global.monitoring.TrackExecutionTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private static final Long ZERO_DISCOUNT_AMOUNT = 0L;
    private static final List<PaymentStatus> ACTIVE_PAYMENT_STATUSES =
            List.of(PaymentStatus.READY, PaymentStatus.PENDING, PaymentStatus.PAID);

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationPaymentRepository reservationPaymentRepository;
    private final PaymentMerchantUidGenerator merchantUidGenerator;
    private final PortOnePaymentClient portOnePaymentClient;
    private final ReservationService reservationService;
    private final ReservationLockService reservationLockService;
    private final CouponService couponService;

    /*
    결제 요청 생성
    PET-129 범위에서는 PortOne 승인 전 READY 상태의 결제 요청만 생성한다.
     */
    @Transactional
    public PaymentResponseDto create(Long memberId, CreatePaymentRequest request) {
        Reservation reservation = findReservation(request.reservationId());
        // CONFIRMED, COMPLETED, CANCELED, EXPIRED 상태면 Payment를 생성할 수 없다
        validateReservationPending(reservation);

        // 지불하려는 사람이 Reservation Party 이고,
        // 중복 지불이 아니며
        // 진행중인 결제가 없는지 확인
        ReservationPayment reservationPayment = findReservationPayment(reservation.getId());
        validatePaymentParty(memberId, reservation, request.paymentRole());
        validateNotAlreadyPaid(reservationPayment, request.paymentRole());
        validateNoActivePayment(reservation.getId(), memberId, request.paymentRole());

        Long originalAmount = getOriginalAmount(reservationPayment, request.paymentRole());
        PaymentAmount paymentAmount = calculatePaymentAmount(memberId, request.paymentRole(), originalAmount);

        Payment payment = paymentRepository.save(Payment.builder()
                .reservationId(reservation.getId())
                .memberId(memberId)
                .paymentRole(request.paymentRole())
                .paymentType(getPaymentType(request.paymentRole()))
                .originalAmount(originalAmount)
                .discountAmount(paymentAmount.discountAmount())
                .finalAmount(paymentAmount.finalAmount())
                .userCouponId(paymentAmount.userCouponId())
                .provider(PaymentProvider.PORTONE)
                .merchantUid(merchantUidGenerator.generate(reservation.getId(), request.paymentRole()))
                .build());

        log.info("[PaymentService] 결제 요청 생성 paymentId={}, reservationId={}, role={}",
                payment.getId(), reservation.getId(), request.paymentRole());

        return PaymentResponseDto.from(payment);
    }

    /*
    결제 승인 검증
    PortOne V2에서는 서버가 발급한 merchantUid 값을 PortOne paymentId로 사용한다.
    결제 금액은 프론트 요청값이 아니라 Payment.finalAmount와 PortOne 조회 결과를 비교한다.
     */
    @TrackExecutionTime("payment.confirm")
    @Transactional
    public ConfirmPaymentResponse confirm(Long memberId, ConfirmPaymentRequest request) {
        // 프론트에서 자동으로 요청하는 api
        Payment payment = paymentRepository.findByMerchantUid(request.merchantUid())
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        validatePaymentOwner(memberId, payment);

        return reservationLockService.executeWithReservationLock(
                payment.getReservationId(), () -> confirmPayment(payment));
    }

    @Transactional
    public ConfirmPaymentResponse confirmByWebhook(String merchantUid) {
        // webhook 으로 요청해서 결과값을 직접 받아오는 api
        Payment payment = paymentRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        return reservationLockService.executeWithReservationLock(
                payment.getReservationId(), () -> confirmPayment(payment));
    }

    @Transactional
    public PaymentResponseDto failByWebhook(String merchantUid, String failedReason) {
        Payment payment = paymentRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        return reservationLockService.executeWithReservationLock(payment.getReservationId(), () -> {
            if (!payment.isFailable()) {
                return PaymentResponseDto.from(payment);
            }

            payment.fail(failedReason);
            restoreCouponIfApplied(payment);
            return PaymentResponseDto.from(payment);
        });
    }

    /*
    결제 실패 처리
    프론트 결제창 중단/PG 실패 이후 READY 결제를 닫아 다음 결제 요청을 가능하게 한다.
     */
    @Transactional
    public PaymentResponseDto fail(Long memberId, FailPaymentRequest request) {
        Payment payment = paymentRepository.findByMerchantUid(request.merchantUid())
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        return reservationLockService.executeWithReservationLock(payment.getReservationId(), () -> {
            validatePaymentOwner(memberId, payment);
            validateFailable(payment);

            payment.fail(request.failedReason());
            restoreCouponIfApplied(payment);

            log.info("[PaymentService] 결제 실패 처리 paymentId={}, merchantUid={}, reason={}",
                    payment.getId(), payment.getMerchantUid(), request.failedReason());

            return PaymentResponseDto.from(payment);
        });
    }

    public List<PaymentResponseDto> getMyPayments(Long memberId) {
        return paymentRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(PaymentResponseDto::from)
                .toList();
    }

    public PaymentResponseDto getDetail(Long memberId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        validatePaymentOwner(memberId, payment);

        return PaymentResponseDto.from(payment);
    }

    private Reservation findReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));
    }

    private ReservationPayment findReservationPayment(Long reservationId) {
        return reservationPaymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));
    }

    private void validateReservationPending(Reservation reservation) {
        if (!reservation.isPending()) {
            throw new ReservationException(ReservationErrorCode.INVALID_RESERVATION_STATUS_TRANSITION);
        }
    }

    private void validatePaymentParty(Long memberId, Reservation reservation, PaymentRole paymentRole) {
        if (paymentRole == PaymentRole.GUARDIAN && !reservation.isGuardian(memberId)) {
            throw new PaymentException(PaymentErrorCode.NOT_PAYMENT_PARTY);
        }

        if (paymentRole == PaymentRole.SITTER && !reservation.isSitter(memberId)) {
            throw new PaymentException(PaymentErrorCode.NOT_PAYMENT_PARTY);
        }
    }

    private void validatePaymentOwner(Long memberId, Payment payment) {
        if (!payment.getMemberId().equals(memberId)) {
            throw new PaymentException(PaymentErrorCode.NOT_PAYMENT_PARTY);
        }
    }

    private void validateConfirmable(Payment payment) {
        if (!payment.isConfirmable()) {
            throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS);
        }
    }

    private void validateFailable(Payment payment) {
        if (!payment.isFailable()) {
            throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS);
        }
    }

    private ConfirmPaymentResponse confirmPayment(Payment payment) {
        if (payment.isPaid()) {
            Reservation reservation = findReservation(payment.getReservationId());
            return ConfirmPaymentResponse.of(payment, reservation.getStatus());
        }

        // PaymentStatus == READY || PENDING 일 때만 Confirm
        validateConfirmable(payment);

        PortOnePaymentResult portOnePayment = portOnePaymentClient.getPayment(payment.getMerchantUid());

        // merchantUid 체크, 금액 검증, 결제가 실제로 완료되었는지 확인
        validatePortOnePayment(payment, portOnePayment);

        /* payment PAID 처리
        ReservationPayment 도 Paid 처리 (reservationId 를 기준으로 검색해서 결제한 Role 만)
         */
        payment.approve(portOnePayment.paymentId(), portOnePayment.rawResponse());
        markCouponAsUsedIfApplied(payment);

        ReservationPayment reservationPayment = findReservationPayment(payment.getReservationId());
        markReservationPaymentPaid(reservationPayment, payment.getPaymentRole());

        /*
        confirm 후속처리
        - reservation CONFIRMED 처리 : Lock 걸어서 만약 충돌나면 바로 Cancel 처리
        - 역방향: 나머지 PENDING 제안 → REJECTED, 공고 → CLOSED, 시터의 Proposal → WITHDRAWN
         */
        ReservationResponseDto reservation = reservationService.confirmAfterPayment(payment.getReservationId());

        log.info("[PaymentService] 결제 승인 완료 paymentId={}, reservationId={}, role={}",
                payment.getId(), payment.getReservationId(), payment.getPaymentRole());

        return ConfirmPaymentResponse.of(payment, reservation.status());
    }

    /*
    1. 내가 지금 가지고 있는 merchantUid 랑 PortOnePaymentResult 에 저장된 paymentId 와 동일한지
        (PortOnePayment 에 있는 paymentId 는 merchantUid이다)
    2. 금액 검증: FinalAmount 가 result 의 totalAmount 랑 동일한지
    3. result 에 PAID 처리가 되어있는지
     */
    private void validatePortOnePayment(Payment payment, PortOnePaymentResult portOnePayment) {
        if (!payment.getMerchantUid().equals(portOnePayment.paymentId())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_ID_MISMATCH);
        }

        if (!payment.getFinalAmount().equals(portOnePayment.totalAmount())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        if (!portOnePayment.isPaid()) {
            throw new PaymentException(PaymentErrorCode.PORTONE_PAYMENT_NOT_PAID);
        }
    }

    /*
    Reservation Payment 지불한 역할에 대해서만 Paid 처리
     */
    private void markReservationPaymentPaid(ReservationPayment reservationPayment, PaymentRole paymentRole) {
        if (paymentRole == PaymentRole.GUARDIAN) {
            reservationPayment.guardianConfirm();
            return;
        }
        reservationPayment.sitterConfirm();
    }

    private void validateNotAlreadyPaid(ReservationPayment reservationPayment, PaymentRole paymentRole) {
        if (paymentRole == PaymentRole.GUARDIAN && reservationPayment.isGuardianPaid()) {
            throw new ReservationException(ReservationErrorCode.ALREADY_PAID);
        }

        if (paymentRole == PaymentRole.SITTER && reservationPayment.isSitterPaid()) {
            throw new ReservationException(ReservationErrorCode.ALREADY_PAID);
        }
    }

    private void validateNoActivePayment(Long reservationId, Long memberId, PaymentRole paymentRole) {
        boolean exists = paymentRepository.existsByReservationIdAndMemberIdAndPaymentRoleAndStatusIn(
                reservationId, memberId, paymentRole, ACTIVE_PAYMENT_STATUSES);

        if (exists) {
            throw new PaymentException(PaymentErrorCode.DUPLICATE_PAYMENT_REQUEST);
        }
    }

    private Long getOriginalAmount(ReservationPayment reservationPayment, PaymentRole paymentRole) {
        if (paymentRole == PaymentRole.GUARDIAN) {
            return (long) reservationPayment.getGuardianPrice();
        }
        return (long) reservationPayment.getSitterPrice();
    }

    private PaymentAmount calculatePaymentAmount(Long memberId, PaymentRole paymentRole, Long originalAmount) {
        if (paymentRole == PaymentRole.SITTER) {
            return PaymentAmount.withoutCoupon(originalAmount);
        }

        Long userCouponId = findUsableCouponId(memberId);
        if (userCouponId == null) {
            return PaymentAmount.withoutCoupon(originalAmount);
        }

        CouponApplyResult couponResult = couponService.applyCoupon(memberId, userCouponId, originalAmount);
        return new PaymentAmount(
                couponResult.discountAmount(),
                couponResult.finalPrice(),
                userCouponId
        );
    }

    private Long findUsableCouponId(Long memberId) {
        return couponService.findActiveUserCouponIds(memberId).stream()
                .filter(userCouponId -> !paymentRepository.existsByUserCouponIdAndStatusIn(
                        userCouponId,
                        ACTIVE_PAYMENT_STATUSES
                ))
                .findFirst()
                .orElse(null);
    }

    private void markCouponAsUsedIfApplied(Payment payment) {
        if (payment.getUserCouponId() != null) {
            couponService.markCouponAsUsed(payment.getMemberId(), payment.getUserCouponId());
        }
    }

    private void restoreCouponIfApplied(Payment payment) {
        if (payment.getUserCouponId() != null) {
            couponService.restoreCoupon(payment.getMemberId(), payment.getUserCouponId());
        }
    }

    private PaymentType getPaymentType(PaymentRole paymentRole) {
        if (paymentRole == PaymentRole.GUARDIAN) {
            return PaymentType.FULL;
        }
        return PaymentType.DEPOSIT;
    }

    private record PaymentAmount(
            Long discountAmount,
            Long finalAmount,
            Long userCouponId
    ) {
        private static PaymentAmount withoutCoupon(Long originalAmount) {
            return new PaymentAmount(ZERO_DISCOUNT_AMOUNT, originalAmount, null);
        }
    }
}
