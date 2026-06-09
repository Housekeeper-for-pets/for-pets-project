package com.forpets.domain.reservation.service;

import com.forpets.domain.carerequest.entity.CareRequest;
import com.forpets.domain.carerequest.repository.CareRequestRepository;
import com.forpets.domain.notification.broker.NotificationMessageBroker;
import com.forpets.domain.notification.event.NotificationEvent;
import com.forpets.domain.payment.service.PaymentRefundService;
import com.forpets.domain.proposal.entity.Proposal;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationSource;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.global.sse.SseEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
호출 흐름
 ReservationExpireScheduler
 -> ReservationLockService.executeWithReservationLock(...)
 -> ReservationExpireService.expireOne(reservationId)   :여기서부터 Transactional

 reservation Lock service 부터 Transaction 을 걸면 Reservation 하나만 Lock 을 획득하지 못하거나 어디선가 오류가 발생하면
 모든 애들이 Rollback 됨을 막기 위함

 Lock 도 트랜잭션 밖에서 잡기 때문에 여기선 잡지않음
 Lock 해제 -> 커밋 시간차 문제 방지

내부 흐름:
 1) reservation 재조회 - Lock 획득 ~ 트랜잭션 시작 사이에 상태가 바뀌었을 수 있음
 2) 상태 가드        - PENDING 이 아니면 skip (reservation.expire() 안에도 가드 있음)
 3) reservation.expire()                : PENDING -> EXPIRED
 4) refundPaidPayments                   : PAID -> REFUNDED, 쿠폰 복구
 5) expireNonPaidPayments                : READY/PENDING -> EXPIRED
 6) restoreSource                        : Proposal/CareRequest ACCEPTED -> PENDING
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationExpireService {

    private static final String EXPIRE_REFUND_REASON = "예약 결제 제한 시간 초과";

    private final ReservationRepository reservationRepository;
    private final PaymentRefundService paymentRefundService;
    private final ProposalRepository proposalRepository;
    private final CareRequestRepository careRequestRepository;
    private final NotificationMessageBroker notificationBroker;

    @Transactional
    public void expireOne(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElse(null);
        if (reservation == null) {
            log.warn("[ReservationExpireService] reservation 이 존재하지 않음 reservationId={}", reservationId);
            return;
        }

        // Lock 잡기 직전 ~ 트랜잭션 시작 사이에 Confirm/Cancel 이 먼저 처리됐을 수 있음
        if (!reservation.isPending()) {
            log.info("[ReservationExpireService] Pending 이 아님  -> skip reservationId={}, status={}",
                    reservationId, reservation.getStatus());
            return;
        }

        reservation.expire();
        paymentRefundService.refundPaidPayments(reservationId, EXPIRE_REFUND_REASON);
        paymentRefundService.expireNonPaidPayments(reservationId);
        restoreSource(reservation);
        sendReservationExpiredNotifications(reservation);

        log.info("[ReservationExpireService] 만료 처리 완료 reservationId={}", reservationId);
    }

    private void sendReservationExpiredNotifications(Reservation reservation) {
        notificationBroker.publish(NotificationEvent.of(
                reservation.getGuardianId(),
                null,
                SseEventType.RESERVATION_EXPIRED,
                "예약 결제 제한 시간이 초과되어 예약이 만료되었습니다.",
                reservation.getId(),
                "RESERVATION"
        ));

        notificationBroker.publish(NotificationEvent.of(
                reservation.getSitterMemberId(),
                null,
                SseEventType.RESERVATION_EXPIRED,
                "예약 결제 제한 시간이 초과되어 예약이 만료되었습니다.",
                reservation.getId(),
                "RESERVATION"
        ));

        log.info("[ReservationExpireService] 예약 만료 알림 발행 reservationId={}, guardianId={}, sitterMemberId={}",
                reservation.getId(), reservation.getGuardianId(), reservation.getSitterMemberId());
    }

    private void restoreSource(Reservation reservation) {
        if (reservation.getSource() == ReservationSource.PROPOSAL) {
            proposalRepository.findById(reservation.getSourceId())
                    .ifPresent(Proposal::restoreToPending);
            return;
        }
        careRequestRepository.findById(reservation.getSourceId())
                .ifPresent(CareRequest::restoreToPending);
    }
}
