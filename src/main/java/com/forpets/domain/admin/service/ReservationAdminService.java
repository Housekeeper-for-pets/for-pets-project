package com.forpets.domain.admin.service;

import com.forpets.domain.payment.service.PaymentRefundService;
import com.forpets.domain.notification.broker.NotificationMessageBroker;
import com.forpets.domain.notification.event.NotificationEvent;
import com.forpets.domain.reservation.dto.ReservationPageResponse;
import com.forpets.domain.reservation.dto.ReservationResponseDto;
import com.forpets.domain.reservation.entity.*;
import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import com.forpets.domain.reservation.repository.ReservationPaymentRepository;
import com.forpets.domain.reservation.repository.ReservationPetRepository;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.domain.reservation.repository.ReservationTimeSlotRepository;
import com.forpets.global.sse.SseEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final NotificationMessageBroker notificationBroker;

    /*
    CANCEL_REQUESTED 상태이고 UNAVOIDABLE 사유인 취소 요청 목록 (페이징)
    다른 목록 조회 API (Post, Sitter 등) 의 페이지네이션 컨벤션과 일치
    - 최신 신청 순 (updatedAt DESC) 정렬
     */
    public ReservationPageResponse getCancelRequests(int page, int size) {
        validatePageRequest(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Reservation> requestPage = reservationRepository
                .findAllByStatusAndCancelCategory(
                        ReservationStatus.CANCEL_REQUESTED,
                        CancelCategory.UNAVOIDABLE,
                        pageable);

        List<ReservationResponseDto> content = requestPage.getContent().stream()
                .map(this::toResponseDto)
                .toList();

        return ReservationPageResponse.of(
                content,
                requestPage.getTotalElements(),
                requestPage.getTotalPages(),
                requestPage.getNumber(),
                requestPage.getSize()
        );
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
        sendReservationCanceledNotifications(reservation);

        return toResponseDto(reservation);
    }

    // 요청 거절
    @Transactional
    public ReservationResponseDto reject(Long reservationId) {
        Reservation reservation = findById(reservationId);
        validateCancelRequested(reservation);

        Long requesterId = getCancelRequesterId(reservation);
        reservation.restoreToConfirmed();
        log.info("[관리자 취소 거절] reservationId={}, CONFIRMED 복원", reservationId);
        sendReservationCancelRejectedNotification(reservation, requesterId);

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

    private void validatePageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new ReservationException(ReservationErrorCode.INVALID_PAGE_REQUEST);
        }
    }

    private void sendReservationCanceledNotifications(Reservation reservation) {
        notificationBroker.publish(NotificationEvent.of(
                reservation.getGuardianId(),
                null,
                SseEventType.RESERVATION_CANCELED,
                "예약이 취소되었습니다.",
                reservation.getId(),
                "RESERVATION"
        ));

        notificationBroker.publish(NotificationEvent.of(
                reservation.getSitterMemberId(),
                null,
                SseEventType.RESERVATION_CANCELED,
                "예약이 취소되었습니다.",
                reservation.getId(),
                "RESERVATION"
        ));

        log.info("[ReservationAdminService] 예약 취소 승인 알림 발행 reservationId={}, guardianId={}, sitterMemberId={}",
                reservation.getId(), reservation.getGuardianId(), reservation.getSitterMemberId());
    }

    private Long getCancelRequesterId(Reservation reservation) {
        if (reservation.getCanceledBy() == CanceledBy.GUARDIAN) {
            return reservation.getGuardianId();
        }
        return reservation.getSitterMemberId();
    }

    private void sendReservationCancelRejectedNotification(Reservation reservation, Long requesterId) {
        notificationBroker.publish(NotificationEvent.of(
                requesterId,
                null,
                SseEventType.RESERVATION_CANCEL_REJECTED,
                "예약 취소 요청이 거절되었습니다.",
                reservation.getId(),
                "RESERVATION"
        ));

        log.info("[ReservationAdminService] 예약 취소 요청 거절 알림 발행 reservationId={}, requesterId={}",
                reservation.getId(), requesterId);
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
