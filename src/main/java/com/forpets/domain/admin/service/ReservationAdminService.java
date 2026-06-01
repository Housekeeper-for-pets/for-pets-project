package com.forpets.domain.admin.service;

import com.forpets.domain.payment.service.PaymentRefundService;
import com.forpets.domain.reservation.dto.ReservationResponseDto;
import com.forpets.domain.reservation.entity.*;
import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import com.forpets.domain.reservation.repository.ReservationPaymentRepository;
import com.forpets.domain.reservation.repository.ReservationPetRepository;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.domain.reservation.repository.ReservationTimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationAdminService {
    private final ReservationRepository reservationRepository;
    private final ReservationPaymentRepository reservationPaymentRepository;
    private final ReservationPetRepository reservationPetRepository;
    private final ReservationTimeSlotRepository reservationTimeSlotRepository;
    private final PaymentRefundService paymentRefundService;

    // CANCEL_REQUESTED 상태이고, 불가피한 요청으로 취소가 들어온 경우만 조회
    public List<ReservationResponseDto> getCancelRequests() {
        List<Reservation> requests = reservationRepository
                .findAllByStatusAndCancelCategory(
                        ReservationStatus.CANCEL_REQUESTED,
                        CancelCategory.UNAVOIDABLE);

        return requests.stream()
                .map(this::toResponseDto)
                .toList();
    }

    // 요청 승인
    @Transactional
    public ReservationResponseDto approve(Long reservationId) {
        Reservation reservation = findById(reservationId);
        validateCancelRequested(reservation);

        reservation.cancel(
                reservation.getCancelReason(),
                reservation.getCancelCategory(),
                reservation.getCanceledBy());

        log.info("[관리자 취소 승인] reservationId={}, 전액 환불 처리", reservationId);

        paymentRefundService.refundPaidPayments(reservationId, reservation.getCancelReason());
        paymentRefundService.expireNonPaidPayments(reservationId);

        return toResponseDto(reservation);
    }

    // 요청 거절
    @Transactional
    public ReservationResponseDto reject(Long reservationId) {
        Reservation reservation = findById(reservationId);
        validateCancelRequested(reservation);

        reservation.restoreToConfirmed();
        log.info("[관리자 취소 거절] reservationId={}, CONFIRMED 복원", reservationId);

        return toResponseDto(reservation);
    }

    private Reservation findById(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException(
                        ReservationErrorCode.RESERVATION_NOT_FOUND));
    }

    private void validateCancelRequested(Reservation reservation) {
        if (!reservation.isCancelRequested()) {
            throw new ReservationException(
                    ReservationErrorCode.INVALID_RESERVATION_STATUS_TRANSITION);
        }
    }

    private ReservationResponseDto toResponseDto(Reservation reservation) {
        ReservationPayment payment = reservationPaymentRepository
                .findByReservationId(reservation.getId())
                .orElseThrow(() -> new ReservationException(
                        ReservationErrorCode.RESERVATION_NOT_FOUND));
        List<ReservationPet> pets = reservationPetRepository
                .findAllByReservationId(reservation.getId());
        List<ReservationTimeSlot> timeSlots = reservationTimeSlotRepository
                .findAllByReservationIdOrderByTimeSlotInfoSequence(reservation.getId());
        return ReservationResponseDto.from(reservation, payment, pets, timeSlots);

    }
}
