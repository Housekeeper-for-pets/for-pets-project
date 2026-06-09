package com.forpets.domain.reservation.service;

import com.forpets.domain.carerequest.repository.CareRequestRepository;
import com.forpets.domain.notification.broker.NotificationMessageBroker;
import com.forpets.domain.payment.service.PaymentRefundService;
import com.forpets.domain.proposal.entity.Proposal;
import com.forpets.domain.proposal.entity.ProposalStatus;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationSource;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.global.common.CareType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationExpireService — 단일 예약 만료 처리")
class ReservationExpireServiceTest {

    @InjectMocks
    private ReservationExpireService reservationExpireService;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaymentRefundService paymentRefundService;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private CareRequestRepository careRequestRepository;

    @Mock
    private NotificationMessageBroker notificationBroker;

    // ── 픽스처 ──
    private Reservation reservation;          // CareRequest 출처 PENDING
    private Reservation proposalReservation;  // Proposal 출처 PENDING
    private Proposal proposal;                // ACCEPTED

    private final Long member1Id = 1L;
    private final Long member2Id = 2L;
    private final Long sitterProfileId = 100L;
    private final Long reservationId = 600L;
    private final Long proposalReservationId = 601L;
    private final Long proposalId = 500L;
    private final Long careRequestSourceId = 200L;

    @BeforeEach
    void setUp() {
        reservation = Reservation.builder()
                .guardianId(member1Id)
                .sitterMemberId(member2Id)
                .sitterProfileId(sitterProfileId)
                .careType(CareType.VISIT)
                .source(ReservationSource.CARE_REQUEST)
                .sourceId(careRequestSourceId)
                .build();
        ReflectionTestUtils.setField(reservation, "id", reservationId);

        proposalReservation = Reservation.builder()
                .guardianId(member1Id)
                .sitterMemberId(member2Id)
                .sitterProfileId(sitterProfileId)
                .careType(CareType.VISIT)
                .source(ReservationSource.PROPOSAL)
                .sourceId(proposalId)
                .build();
        ReflectionTestUtils.setField(proposalReservation, "id", proposalReservationId);

        proposal = Proposal.builder()
                .postId(300L)
                .sitterProfileId(sitterProfileId)
                .memberId(member2Id)
                .proposedPrice(25000)
                .message("잘 돌봐드리겠습니다")
                .build();
        ReflectionTestUtils.setField(proposal, "id", proposalId);
        proposal.accept();
    }

    @Test
    @DisplayName("[성공] PENDING 예약 만료 처리 → status=EXPIRED + 환불 호출")
    void expire_pending_reservation() {
        // given
        given(reservationRepository.findById(reservationId))
                .willReturn(Optional.of(reservation));

        // when
        reservationExpireService.expireOne(reservationId);

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        then(paymentRefundService).should()
                .refundPaidPayments(reservationId, "예약 결제 제한 시간 초과");
        then(paymentRefundService).should()
                .expireNonPaidPayments(reservationId);
        then(notificationBroker).should(org.mockito.Mockito.times(2))
                .publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("[성공] Proposal 출처 예약 만료 시 ACCEPTED → PENDING 복원")
    void restore_proposal_on_expire() {
        // given
        given(reservationRepository.findById(proposalReservationId))
                .willReturn(Optional.of(proposalReservation));
        given(proposalRepository.findById(proposalId))
                .willReturn(Optional.of(proposal));

        // when
        reservationExpireService.expireOne(proposalReservationId);

        // then
        assertThat(proposalReservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.PENDING);
    }

    @Test
    @DisplayName("[스킵] reservation 이 존재하지 않으면 조용히 종료")
    void skip_when_reservation_not_found() {
        // given
        given(reservationRepository.findById(reservationId))
                .willReturn(Optional.empty());

        // when
        reservationExpireService.expireOne(reservationId);

        // then — 어떤 후속 처리도 호출되지 않음
        then(paymentRefundService).shouldHaveNoInteractions();
        then(proposalRepository).shouldHaveNoInteractions();
        then(careRequestRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("[스킵] reservation 이 PENDING 이 아니면 (Confirm/Cancel 선행) 환불 호출 X")
    void skip_when_not_pending() {
        // given — 다른 트랜잭션이 먼저 CONFIRMED 로 만든 상황
        reservation.confirm();
        given(reservationRepository.findById(reservationId))
                .willReturn(Optional.of(reservation));

        // when
        reservationExpireService.expireOne(reservationId);

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        then(paymentRefundService).shouldHaveNoInteractions();
    }
}
