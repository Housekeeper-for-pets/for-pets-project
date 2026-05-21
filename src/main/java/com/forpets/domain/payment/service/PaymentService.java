package com.forpets.domain.payment.service;

import com.forpets.domain.payment.client.PortOnePaymentClient;
import com.forpets.domain.payment.client.PortOnePaymentResult;
import com.forpets.domain.payment.dto.ConfirmPaymentRequest;
import com.forpets.domain.payment.dto.ConfirmPaymentResponse;
import com.forpets.domain.payment.dto.CreatePaymentRequest;
import com.forpets.domain.payment.dto.PaymentResponseDto;
import com.forpets.domain.payment.entity.*;
import com.forpets.domain.payment.exception.PaymentErrorCode;
import com.forpets.domain.payment.exception.PaymentException;
import com.forpets.domain.payment.repository.PaymentRepository;
import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationPayment;
import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import com.forpets.domain.reservation.repository.ReservationPaymentRepository;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.domain.reservation.service.ReservationService;
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

    /*
    결제 요청 생성
    PET-129 범위에서는 PortOne 승인 전 READY 상태의 결제 요청만 생성한다.
     */
    @Transactional
    public PaymentResponseDto create(Long memberId, CreatePaymentRequest request) {
        Reservation reservation = findReservation(request.reservationId());
        validateReservationPending(reservation);

        ReservationPayment reservationPayment = findReservationPayment(reservation.getId());
        validatePaymentParty(memberId, reservation, request.paymentRole());
        validateNotAlreadyPaid(reservationPayment, request.paymentRole());
        validateNoActivePayment(reservation.getId(), memberId, request.paymentRole());

        Long originalAmount = getOriginalAmount(reservationPayment, request.paymentRole());
        Long discountAmount = ZERO_DISCOUNT_AMOUNT;
        Long finalAmount = originalAmount - discountAmount;

        Payment payment = paymentRepository.save(Payment.builder()
                .reservationId(reservation.getId())
                .memberId(memberId)
                .paymentRole(request.paymentRole())
                .paymentType(getPaymentType(request.paymentRole()))
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .userCouponId(null)
                .provider(PaymentProvider.PORTONE)
                .merchantUid(merchantUidGenerator.generate(reservation.getId(), request.paymentRole()))
                .build());

        log.info("[PaymentService] 결제 요청 생성 paymentId={}, reservationId={}, role={}",
                payment.getId(), reservation.getId(), request.paymentRole());

        return PaymentResponseDto.from(payment);
    }

    /*
    결제 승인 검증
    PortOne V2에서는 merchantUid 값을 paymentId로 사용한다.
    결제 금액은 프론트 요청값이 아니라 Payment.finalAmount와 PortOne 조회 결과를 비교한다.
     */
    @Transactional
    public ConfirmPaymentResponse confirm(Long memberId, ConfirmPaymentRequest request) {
        Payment payment = paymentRepository.findByMerchantUid(request.merchantUid())
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        validatePaymentOwner(memberId, payment);
        validateConfirmable(payment);

        PortOnePaymentResult portOnePayment = portOnePaymentClient.getPayment(payment.getMerchantUid());
        validatePortOnePayment(payment, portOnePayment);

        payment.approve(portOnePayment.paymentId(), portOnePayment.rawResponse());
        ReservationPayment reservationPayment = findReservationPayment(payment.getReservationId());
        markReservationPaymentPaid(reservationPayment, payment.getPaymentRole());

        var reservation = reservationService.confirmAfterPayment(payment.getReservationId());

        log.info("[PaymentService] 결제 승인 완료 paymentId={}, reservationId={}, role={}",
                payment.getId(), payment.getReservationId(), payment.getPaymentRole());

        return ConfirmPaymentResponse.of(payment, reservation.status());
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

    private PaymentType getPaymentType(PaymentRole paymentRole) {
        if (paymentRole == PaymentRole.GUARDIAN) {
            return PaymentType.FULL;
        }
        return PaymentType.DEPOSIT;
    }
}
