package com.forpets.domain.reservation.service;

import com.forpets.domain.carelog.repository.CareLogRepository;
import com.forpets.domain.carerequest.repository.CareRequestRepository;
import com.forpets.domain.notification.broker.NotificationMessageBroker;
import com.forpets.domain.payment.entity.Payment;
import com.forpets.domain.payment.entity.PaymentProvider;
import com.forpets.domain.payment.entity.PaymentRole;
import com.forpets.domain.payment.entity.PaymentStatus;
import com.forpets.domain.payment.entity.PaymentType;
import com.forpets.domain.payment.exception.PaymentErrorCode;
import com.forpets.domain.payment.exception.PaymentException;
import com.forpets.domain.payment.repository.PaymentRepository;
import com.forpets.domain.proposal.entity.Proposal;
import com.forpets.domain.proposal.entity.ProposalStatus;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.domain.payment.service.PaymentRefundService;
import com.forpets.domain.reservation.dto.CancelReservationRequest;
import com.forpets.domain.reservation.dto.ReservationResponseDto;
import com.forpets.domain.reservation.entity.*;
import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import com.forpets.domain.reservation.repository.ReservationPaymentRepository;
import com.forpets.domain.reservation.repository.ReservationPetRepository;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.domain.reservation.repository.ReservationTimeSlotRepository;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.post.repository.PostTimeSlotRepository;
import com.forpets.domain.settlement.service.SettlementService;
import com.forpets.global.common.CareType;
import com.forpets.global.embed.TimeSlotValidator;
import com.forpets.global.embed.entity.TimeSlotInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @InjectMocks
    private ReservationService reservationService;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationPetRepository reservationPetRepository;

    @Mock
    private ReservationTimeSlotRepository reservationTimeSlotRepository;

    @Mock
    private ReservationPaymentRepository reservationPaymentRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentRefundService paymentRefundService;

    @Mock
    private SettlementService settlementService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostTimeSlotRepository postTimeSlotRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private TimeSlotValidator timeSlotValidator;

    // Reservation Lock 을 추가하면서 executeWithSitterLock task 를 그냥 실행하도록 해주기
    @Mock
    private ReservationLockService reservationLockService;

    @Mock
    private CareRequestRepository careRequestRepository;

    // ReservationService.cancel() 에서 케어일지 존재 여부 검증을 위해 의존성 추가됨
    @Mock
    private CareLogRepository careLogRepository;

    @Mock
    private NotificationMessageBroker notificationBroker;

    // ── 테스트 픽스처 ──
    private Reservation reservation;        // PENDING 상태 예약 (CareRequest 출처)
    private Reservation proposalReservation; // PENDING 상태 예약 (Proposal 출처)
    private ReservationPayment payment;     // 초기 결제 정보 (둘 다 미결제)
    private Proposal proposal;              // ACCEPTED 상태 제안

    private final Long member1Id = 1L;      // 째길중 — 보호자
    private final Long member2Id = 2L;      // 타코맘 — 시터
    private final Long member3Id = 3L;      // 지민냥 — 제3자
    private final Long sitterProfileId = 100L;
    private final Long reservationId = 600L;
    private final Long proposalReservationId = 601L;
    private final Long proposalId = 500L;
    private final Long sourceId = 200L;
    private final Long guardianPaymentId = 900L;

    @BeforeEach
    void setUp() {
        // PENDING 예약 — CareRequest 출처
        reservation = Reservation.builder()
                .guardianId(member1Id)
                .sitterMemberId(member2Id)
                .sitterProfileId(sitterProfileId)
                .careType(CareType.VISIT)
                .source(ReservationSource.CARE_REQUEST)
                .sourceId(sourceId)
                .build();
        ReflectionTestUtils.setField(reservation, "id", reservationId);

        // PENDING 예약 — Proposal 출처
        proposalReservation = Reservation.builder()
                .guardianId(member1Id)
                .sitterMemberId(member2Id)
                .sitterProfileId(sitterProfileId)
                .careType(CareType.VISIT)
                .source(ReservationSource.PROPOSAL)
                .sourceId(proposalId)
                .build();
        ReflectionTestUtils.setField(proposalReservation, "id", proposalReservationId);

        // 초기 결제 정보 (둘 다 미결제)
        payment = ReservationPayment.create(reservationId, 30000, 6000);
        ReflectionTestUtils.setField(payment, "id", 1L);

        // ACCEPTED 상태 제안
        proposal = Proposal.builder()
                .postId(300L)
                .sitterProfileId(sitterProfileId)
                .memberId(member2Id)
                .proposedPrice(25000)
                .message("잘 돌봐드리겠습니다")
                .build();
        ReflectionTestUtils.setField(proposal, "id", proposalId);
        proposal.accept(); // ACCEPTED 상태로 변경
    }

    // ── 헬퍼 ──
    private ReservationTimeSlot createTimeSlot(Long reservationId, int sequence, LocalDate date,
                                               LocalTime start, LocalTime end) {
        TimeSlotInfo info = TimeSlotInfo.of(date, start, end, sequence);
        ReservationTimeSlot slot = ReservationTimeSlot.create(reservationId, info);
        ReflectionTestUtils.setField(slot, "id", (long) (sequence + 700));
        return slot;
    }

    private void stubToResponseDto(Long resId, Reservation res, ReservationPayment pmt) {
        given(reservationPaymentRepository.findByReservationId(resId)).willReturn(Optional.of(pmt));
        given(reservationPetRepository.findAllByReservationId(resId)).willReturn(List.of());
        given(reservationTimeSlotRepository.findAllByReservationIdOrderByTimeSlotInfoSequence(resId))
                .willReturn(List.of());
    }

    private Payment createPaidGuardianPayment() {
        Payment guardianPayment = Payment.builder()
                .reservationId(reservationId)
                .memberId(member1Id)
                .paymentRole(PaymentRole.GUARDIAN)
                .paymentType(PaymentType.FULL)
                .originalAmount(30000L)
                .discountAmount(0L)
                .finalAmount(30000L)
                .provider(PaymentProvider.PORTONE)
                .merchantUid("PAY-600-GUARDIAN-TEST")
                .build();
        ReflectionTestUtils.setField(guardianPayment, "id", guardianPaymentId);
        guardianPayment.approve("payment-test", "{}");
        return guardianPayment;
    }

    // ========================================================
    // 내 예약 목록 조회 — GET /api/reservations
    // ========================================================
    @Nested
    @DisplayName("내 예약 목록 조회 — GET /api/reservations")
    class GetMyReservationsTest {

        @Test
        @DisplayName("[성공] 보호자로 참여한 예약 목록 조회 성공")
        void reservation_test_01() {
            // given
            given(reservationRepository.findAllByGuardianId(member1Id)).willReturn(List.of(reservation));
//            given(reservationRepository.findAllBySitterMemberId(member1Id)).willReturn(List.of());
            stubToResponseDto(reservationId, reservation, payment);

            // when
            List<ReservationResponseDto> result = reservationService.getMyReservations(member1Id, ReservationRole.GUARDIAN);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).guardianId()).isEqualTo(member1Id);
        }

        @Test
        @DisplayName("[성공] 시터로 참여한 예약 목록 조회 성공")
        void reservation_test_02() {
            // given
//            given(reservationRepository.findAllByGuardianId(member2Id)).willReturn(List.of());
            given(reservationRepository.findAllBySitterMemberId(member2Id)).willReturn(List.of(reservation));
            stubToResponseDto(reservationId, reservation, payment);

            // when
            List<ReservationResponseDto> result = reservationService.getMyReservations(member2Id, ReservationRole.SITTER);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).sitterMemberId()).isEqualTo(member2Id);
        }
    }

    // ========================================================
    // 예약 상세 조회 — GET /api/reservations/{reservationId}
    // ========================================================
    @Nested
    @DisplayName("예약 상세 조회 — GET /api/reservations/{reservationId}")
    class GetDetailTest {

        @Test
        @DisplayName("[성공] 보호자가 예약 상세 조회 성공")
        void reservation_test_03() {
            // given
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            stubToResponseDto(reservationId, reservation, payment);

            // when
            ReservationResponseDto result = reservationService.getDetail(member1Id, reservationId);

            // then
            assertThat(result.id()).isEqualTo(reservationId);
            assertThat(result.guardianId()).isEqualTo(member1Id);
            assertThat(result.source()).isEqualTo(ReservationSource.CARE_REQUEST);
        }

        @Test
        @DisplayName("[성공] 시터가 예약 상세 조회 성공")
        void reservation_test_04() {
            // given
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            stubToResponseDto(reservationId, reservation, payment);

            // when
            ReservationResponseDto result = reservationService.getDetail(member2Id, reservationId);

            // then
            assertThat(result.id()).isEqualTo(reservationId);
            assertThat(result.sitterMemberId()).isEqualTo(member2Id);
        }

        @Test
        @DisplayName("[실패] 당사자가 아닌 회원이 상세 조회 시 차단")
        void reservation_test_05() {
            // given
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

            // when & then
            assertThatThrownBy(() -> reservationService.getDetail(member3Id, reservationId))
                    .isInstanceOf(ReservationException.class)
                    .satisfies(ex -> assertThat(((ReservationException) ex).getErrorCode())
                            .isEqualTo(ReservationErrorCode.NOT_RESERVATION_PARTY));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 예약 조회")
        void reservation_test_06() {
            // given
            given(reservationRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reservationService.getDetail(member1Id, 999L))
                    .isInstanceOf(ReservationException.class)
                    .satisfies(ex -> assertThat(((ReservationException) ex).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND));
        }
    }

    // ========================================================
    // 예약 확정 — Payment 검증 이후 Reservation CONFIRMED 전환
    // ========================================================
    @Nested
    @DisplayName("예약 확정 — Payment 검증 이후 Reservation CONFIRMED 전환")
    class ConfirmTest {

        @Test
        @DisplayName("[성공] 보호자만 결제 완료 — 아직 PENDING")
        void reservation_test_07() {
            // given
            payment.guardianConfirm();
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            given(reservationPaymentRepository.findByReservationId(reservationId)).willReturn(Optional.of(payment));
            stubToResponseDto(reservationId, reservation, payment);

            // when
            ReservationResponseDto result = reservationService.confirmAfterPayment(reservationId);

            // then
            assertThat(result.guardianPaid()).isTrue();
            assertThat(result.status()).isEqualTo(ReservationStatus.PENDING);
        }

        @Test
        @DisplayName("[성공] 시터만 결제 완료 — 아직 PENDING")
        void reservation_test_08() {
            // given
            payment.sitterConfirm();
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            given(reservationPaymentRepository.findByReservationId(reservationId)).willReturn(Optional.of(payment));
            stubToResponseDto(reservationId, reservation, payment);

            // when
            ReservationResponseDto result = reservationService.confirmAfterPayment(reservationId);

            // then
            assertThat(result.sitterPaid()).isTrue();
            assertThat(result.status()).isEqualTo(ReservationStatus.PENDING);
        }

        @Test
        @DisplayName("[성공] 양측 결제 완료 시 CONFIRMED 전환 성공")
        void reservation_test_09() {
            // given — Payment 도메인에서 양측 결제 완료 처리 후 호출되는 시나리오
            payment.guardianConfirm();
            payment.sitterConfirm();

            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            given(reservationPaymentRepository.findByReservationId(reservationId)).willReturn(Optional.of(payment));
            given(reservationRepository.findAllBySitterProfileIdAndStatus(sitterProfileId, ReservationStatus.CONFIRMED))
                    .willReturn(List.of());
            given(reservationTimeSlotRepository.findAllByReservationIdOrderByTimeSlotInfoSequence(reservationId))
                    .willReturn(List.of());
            given(proposalRepository.findAllBySitterProfileIdAndStatus(sitterProfileId, ProposalStatus.PENDING))
                    .willReturn(List.of());
            stubToResponseDto(reservationId, reservation, payment);

            // 만약에 executeWithSitterLock 동작에 대한 부분이 들어오면 무조건 통과시켜주기!
            given(reservationLockService.executeWithSitterLock(
                    any(), any()))
                    .willAnswer(invocation -> {
                        Supplier<?> task = invocation.getArgument(1);
                        return task.get();  // 락 없이 그냥 실행
                    });

            // when
            ReservationResponseDto result = reservationService.confirmAfterPayment(reservationId);

            // then
            assertThat(result.status()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(result.sitterPaid()).isTrue();
            assertThat(result.guardianPaid()).isTrue();
        }

        @Test
        @DisplayName("[실패] CONFIRMED 예약과 시간 충돌 시 CONFIRMED 전환 차단")
        void reservation_test_12() {
            // given — 양측 결제 완료 상태에서 CONFIRMED 전환 시 충돌 발생
            payment.guardianConfirm();
            payment.sitterConfirm();

            ReservationTimeSlot newSlot = createTimeSlot(
                    reservationId, 1,
                    LocalDate.now().plusDays(3), LocalTime.of(10, 0), LocalTime.of(14, 0));

            Reservation conflictingReservation = Reservation.builder()
                    .guardianId(999L)
                    .sitterMemberId(member2Id)
                    .sitterProfileId(sitterProfileId)
                    .careType(CareType.VISIT)
                    .source(ReservationSource.CARE_REQUEST)
                    .sourceId(999L)
                    .build();
            ReflectionTestUtils.setField(conflictingReservation, "id", 999L);
            conflictingReservation.confirm(); // 이미 CONFIRMED

            ReservationTimeSlot conflictSlot = createTimeSlot(
                    999L, 1,
                    LocalDate.now().plusDays(3), LocalTime.of(11, 0), LocalTime.of(15, 0));

            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            given(reservationPaymentRepository.findByReservationId(reservationId)).willReturn(Optional.of(payment));
            given(reservationRepository.findAllBySitterProfileIdAndStatus(sitterProfileId, ReservationStatus.CONFIRMED))
                    .willReturn(List.of(conflictingReservation));
            given(reservationTimeSlotRepository.findAllByReservationIdOrderByTimeSlotInfoSequence(reservationId))
                    .willReturn(List.of(newSlot));
            given(reservationTimeSlotRepository.findAllByReservationIdOrderByTimeSlotInfoSequence(999L))
                    .willReturn(List.of(conflictSlot));
            given(timeSlotValidator.hasTimeConflict(anyList(), anyList())).willReturn(true);

            // 만약에 executeWithSitterLock 동작에 대한 부분이 들어오면 무조건 통과시켜주기!
            given(reservationLockService.executeWithSitterLock(
                    any(), any()))
                    .willAnswer(invocation -> {
                        Supplier<?> task = invocation.getArgument(1);
                        return task.get();  // 락 없이 그냥 실행
                    });

            // when & then
            assertThatThrownBy(() -> reservationService.confirmAfterPayment(reservationId))
                    .isInstanceOf(ReservationException.class)
                    .satisfies(ex -> assertThat(((ReservationException) ex).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_CONFLICT));
        }

        @Test
        @DisplayName("[실패] PENDING이 아닌 예약 확정 시도")
        void reservation_test_13() {
            // given
            reservation.confirm(); // 이미 CONFIRMED
//            ReflectionTestUtils.setField(payment, "guardianPaid", false); // 결제 상태 초기화

            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
//            given(reservationPaymentRepository.findByReservationId(reservationId)).willReturn(Optional.of(payment));

            // when & then
            assertThatThrownBy(() -> reservationService.confirmAfterPayment(reservationId))
                    .isInstanceOf(ReservationException.class)
                    .satisfies(ex -> assertThat(((ReservationException) ex).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_RESERVATION_STATUS_TRANSITION));
        }
    }

    // ========================================================
    // 케어 완료 — PATCH /api/reservations/{reservationId}/complete
    // ========================================================
    @Nested
    @DisplayName("케어 완료 — PATCH /api/reservations/{reservationId}/complete")
    class CompleteTest {

        // complete() 는 @DistributedLock 어노테이션으로 락을 잡으므로
        // 단위 테스트(AOP 미적용)에서는 별도 stub 불필요

        @Test
        @DisplayName("[성공] 시터가 CONFIRMED 예약 완료 처리 성공")
        void reservation_test_15() {
            // given — 마지막 타임슬롯 종료 시각이 과거인 상황
            reservation.confirm();
            ReservationTimeSlot pastSlot = createTimeSlot(
                    reservationId, 1,
                    LocalDate.now().minusDays(1), LocalTime.of(9, 0), LocalTime.of(12, 0));
            Payment guardianPayment = createPaidGuardianPayment();

            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            // validateCareCompleted + toResponseDto 둘 다 같은 메서드 호출
            // willReturn은 마지막 설정이 이기므로 한 번만 설정하되 pastSlot 반환
            given(reservationTimeSlotRepository.findAllByReservationIdOrderByTimeSlotInfoSequence(reservationId))
                    .willReturn(List.of(pastSlot));
            given(paymentRepository.findByReservationIdAndPaymentRoleAndStatus(
                    reservationId, PaymentRole.GUARDIAN, PaymentStatus.PAID))
                    .willReturn(Optional.of(guardianPayment));
            given(reservationPaymentRepository.findByReservationId(reservationId))
                    .willReturn(Optional.of(payment));
            given(reservationPetRepository.findAllByReservationId(reservationId))
                    .willReturn(List.of());

            // when
            ReservationResponseDto result = reservationService.complete(member2Id, reservationId);

            // then
            assertThat(result.status()).isEqualTo(ReservationStatus.COMPLETED);
            then(settlementService).should().createCareCompletionSettlement(
                    reservationId, member2Id, guardianPaymentId, guardianPayment.getFinalAmount());
            then(paymentRefundService).should().refundSitterDepositAfterCompletion(reservationId);
        }

        @Test
        @DisplayName("[실패] 보호자가 완료 처리 시도")
        void reservation_test_16() {
            // given
            reservation.confirm();
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

            // when & then
            assertThatThrownBy(() -> reservationService.complete(member1Id, reservationId))
                    .isInstanceOf(ReservationException.class)
                    .satisfies(ex -> assertThat(((ReservationException) ex).getErrorCode())
                            .isEqualTo(ReservationErrorCode.NOT_RESERVATION_SITTER));
        }

        @Test
        @DisplayName("[실패] CONFIRMED이 아닌 예약 완료 처리 시도")
        void reservation_test_17() {
            // given — PENDING 상태
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

            // when & then
            assertThatThrownBy(() -> reservationService.complete(member2Id, reservationId))
                    .isInstanceOf(ReservationException.class)
                    .satisfies(ex -> assertThat(((ReservationException) ex).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_RESERVATION_STATUS_TRANSITION));
        }

        @Test
        @DisplayName("[실패] 마지막 타임슬롯 종료 전 완료 처리 시도")
        void reservation_test_18() {
            // given — 마지막 타임슬롯 종료 시각이 미래인 상황
            reservation.confirm();
            ReservationTimeSlot futureSlot = createTimeSlot(
                    reservationId, 1,
                    LocalDate.now().plusDays(3), LocalTime.of(10, 0), LocalTime.of(14, 0));

            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            given(reservationTimeSlotRepository.findAllByReservationIdOrderByTimeSlotInfoSequence(reservationId))
                    .willReturn(List.of(futureSlot));

            // when & then
            assertThatThrownBy(() -> reservationService.complete(member2Id, reservationId))
                    .isInstanceOf(ReservationException.class)
                    .satisfies(ex -> assertThat(((ReservationException) ex).getErrorCode())
                            .isEqualTo(ReservationErrorCode.CARE_NOT_COMPLETED_YET));
        }

        @Test
        @DisplayName("[실패] 보호자 결제 완료 내역이 없으면 완료 처리 차단")
        void reservation_test_18_b() {
            // given
            reservation.confirm();
            ReservationTimeSlot pastSlot = createTimeSlot(
                    reservationId, 1,
                    LocalDate.now().minusDays(1), LocalTime.of(9, 0), LocalTime.of(12, 0));

            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            given(reservationTimeSlotRepository.findAllByReservationIdOrderByTimeSlotInfoSequence(reservationId))
                    .willReturn(List.of(pastSlot));
            given(paymentRepository.findByReservationIdAndPaymentRoleAndStatus(
                    reservationId, PaymentRole.GUARDIAN, PaymentStatus.PAID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reservationService.complete(member2Id, reservationId))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(ex -> assertThat(((PaymentException) ex).getErrorCode())
                            .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND));
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            then(settlementService).should(never())
                    .createCareCompletionSettlement(any(), any(), any(), any());
            then(paymentRefundService).should(never()).refundSitterDepositAfterCompletion(any());
        }
    }

    // ========================================================
// 예약 취소 — PATCH /api/reservations/{reservationId}/cancel
// ========================================================
    @Nested
    @DisplayName("예약 취소 — PATCH /api/reservations/{reservationId}/cancel")
    class CancelTest {

        private final CancelReservationRequest personalCancelRequest = new CancelReservationRequest(
                "개인 사정으로 인해 취소합니다", CancelCategory.PERSONAL
        );

        private final CancelReservationRequest unavoidableCancelRequest = new CancelReservationRequest(
                "병원에 입원하게 되어 어쩔 수 없이 취소합니다", CancelCategory.UNAVOIDABLE
        );

        // cancel() 은 @DistributedLock 어노테이션으로 락을 잡으므로
        // 단위 테스트(AOP 미적용)에서는 별도 stub 불필요

        // ── PENDING 취소 ──

        @Test
        @DisplayName("[성공] 보호자가 PENDING 예약 취소 → 전액 환불")
        void reservation_test_19() {
            // given
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            stubToResponseDto(reservationId, reservation, payment);

            // when
            ReservationResponseDto result = reservationService.cancel(
                    member1Id, reservationId, personalCancelRequest);

            // then
            assertThat(result.status()).isEqualTo(ReservationStatus.CANCELED);
            assertThat(result.canceledBy()).isEqualTo(CanceledBy.GUARDIAN);
            assertThat(result.cancelReason()).isEqualTo("개인 사정으로 인해 취소합니다");
            assertThat(result.cancelCategory()).isEqualTo(CancelCategory.PERSONAL);

            // PENDING 이므로 전액 환불 호출
            then(paymentRefundService).should()
                    .refundPaidPayments(reservationId, personalCancelRequest.cancelReason());
            then(paymentRefundService).should()
                    .cancelNonPaidPayments(reservationId, personalCancelRequest.cancelReason());
            // 위약금 환불은 호출되면 안 됨
            then(paymentRefundService).should(never())
                    .refundWithPenalty(any(), any(), any());
        }

        @Test
        @DisplayName("[성공] 보호자가 PENDING 예약 UNAVOIDABLE 취소 → 전액 환불 (관리자 검토 X)")
        void reservation_test_19_b() {
            // given — PENDING 은 cancelCategory 무관하게 즉시 환불
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            stubToResponseDto(reservationId, reservation, payment);

            // when
            ReservationResponseDto result = reservationService.cancel(
                    member1Id, reservationId, unavoidableCancelRequest);

            // then
            assertThat(result.status()).isEqualTo(ReservationStatus.CANCELED);
            then(paymentRefundService).should()
                    .refundPaidPayments(reservationId, unavoidableCancelRequest.cancelReason());
            then(paymentRefundService).should(never())
                    .refundWithPenalty(any(), any(), any());
        }

        // ── CONFIRMED + 일반 취소 (위약금 차감) ──

        @Test
        @DisplayName("[성공] 시터가 CONFIRMED 예약 PERSONAL 취소 → 위약금 차감 환불")
        void reservation_test_20() {
            // given
            reservation.confirm();
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            stubToResponseDto(reservationId, reservation, payment);

            // when
            ReservationResponseDto result = reservationService.cancel(
                    member2Id, reservationId, personalCancelRequest);

            // then
            assertThat(result.status()).isEqualTo(ReservationStatus.CANCELED);
            assertThat(result.canceledBy()).isEqualTo(CanceledBy.SITTER);

            // CONFIRMED + 일반사유 → 위약금 차감 환불 호출
            then(paymentRefundService).should()
                    .refundWithPenalty(reservationId, personalCancelRequest.cancelReason(), CanceledBy.SITTER);
            then(paymentRefundService).should()
                    .cancelNonPaidPayments(reservationId, personalCancelRequest.cancelReason());
            // 전액환불은 호출되면 안 됨
            then(paymentRefundService).should(never())
                    .refundPaidPayments(any(), any());
        }

        @Test
        @DisplayName("[성공] 보호자가 CONFIRMED 예약 PERSONAL 취소 → 위약금 차감 환불")
        void reservation_test_20_b() {
            // given
            reservation.confirm();
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            stubToResponseDto(reservationId, reservation, payment);

            // when
            ReservationResponseDto result = reservationService.cancel(
                    member1Id, reservationId, personalCancelRequest);

            // then
            assertThat(result.status()).isEqualTo(ReservationStatus.CANCELED);
            assertThat(result.canceledBy()).isEqualTo(CanceledBy.GUARDIAN);

            // canceledBy=GUARDIAN 으로 위약금 차감 호출
            then(paymentRefundService).should()
                    .refundWithPenalty(reservationId, personalCancelRequest.cancelReason(), CanceledBy.GUARDIAN);
            then(paymentRefundService).should(never())
                    .refundPaidPayments(any(), any());
        }

        // ── CONFIRMED + UNAVOIDABLE (관리자 검토 대기) ──

        @Test
        @DisplayName("[성공] 시터가 CONFIRMED 예약 UNAVOIDABLE 취소 요청 → CANCEL_REQUESTED")
        void reservation_test_20_c() {
            // given
            reservation.confirm();
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            stubToResponseDto(reservationId, reservation, payment);

            // when
            ReservationResponseDto result = reservationService.cancel(
                    member2Id, reservationId, unavoidableCancelRequest);

            // then — 상태는 CANCEL_REQUESTED, 환불은 아직 호출 X
            assertThat(result.status()).isEqualTo(ReservationStatus.CANCEL_REQUESTED);
            assertThat(result.canceledBy()).isEqualTo(CanceledBy.SITTER);
            assertThat(result.cancelCategory()).isEqualTo(CancelCategory.UNAVOIDABLE);
            assertThat(result.cancelReason()).isEqualTo("병원에 입원하게 되어 어쩔 수 없이 취소합니다");

            // 어떤 환불 메서드도 호출되면 안 됨
            then(paymentRefundService).should(never())
                    .refundPaidPayments(any(), any());
            then(paymentRefundService).should(never())
                    .refundWithPenalty(any(), any(), any());
            then(paymentRefundService).should(never())
                    .cancelNonPaidPayments(any(), any());
        }

        @Test
        @DisplayName("[성공] 보호자가 CONFIRMED 예약 UNAVOIDABLE 취소 요청 → CANCEL_REQUESTED")
        void reservation_test_20_d() {
            // given
            reservation.confirm();
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            stubToResponseDto(reservationId, reservation, payment);

            // when
            ReservationResponseDto result = reservationService.cancel(
                    member1Id, reservationId, unavoidableCancelRequest);

            // then
            assertThat(result.status()).isEqualTo(ReservationStatus.CANCEL_REQUESTED);
            assertThat(result.canceledBy()).isEqualTo(CanceledBy.GUARDIAN);

            then(paymentRefundService).shouldHaveNoInteractions();
        }

        // ── 후속 처리 ──

        @Test
        @DisplayName("[성공] Proposal 출처 취소 시 ACCEPTED → PENDING 복원")
        void reservation_test_21() {
            // given — PENDING 상태로 취소 (handlePostCancellation 호출 경로)
            given(reservationRepository.findById(proposalReservationId))
                    .willReturn(Optional.of(proposalReservation));
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            stubToResponseDto(proposalReservationId, proposalReservation, payment);

            // when
            reservationService.cancel(member1Id, proposalReservationId, personalCancelRequest);

            // then
            assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.PENDING);
        }

        @Test
        @DisplayName("[성공] CONFIRMED + UNAVOIDABLE 취소 요청 시에는 handlePostCancellation 호출 X (예약 살아있음)")
        void reservation_test_21_b() {
            // given — Proposal 출처지만 UNAVOIDABLE 요청이라 아직 취소 확정 아님
            proposalReservation.confirm();
            given(reservationRepository.findById(proposalReservationId))
                    .willReturn(Optional.of(proposalReservation));
            stubToResponseDto(proposalReservationId, proposalReservation, payment);

            // when
            reservationService.cancel(member1Id, proposalReservationId, unavoidableCancelRequest);

            // then — Proposal 상태 그대로 (관리자 승인 전까진 손대지 않음)
            assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
            // Proposal 조회 자체가 호출되면 안 됨
            then(proposalRepository).should(never()).findById(anyLong());
        }

        @Test
        @DisplayName("[성공] CareRequest 출처 취소 시 별도 후속 처리 없음")
        void reservation_test_22() {
            // given — CARE_REQUEST 출처
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            stubToResponseDto(reservationId, reservation, payment);

            // when
            reservationService.cancel(member1Id, reservationId, personalCancelRequest);

            // then
            then(proposalRepository).should(never()).findById(anyLong());
        }

        // ── 실패 케이스 ──

        @Test
        @DisplayName("[실패] COMPLETED 예약 취소 시도")
        void reservation_test_23() {
            // given
            reservation.confirm();
            reservation.complete(); // COMPLETED 상태
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

            // when & then
            assertThatThrownBy(() -> reservationService.cancel(
                    member1Id, reservationId, personalCancelRequest))
                    .isInstanceOf(ReservationException.class)
                    .satisfies(ex -> assertThat(((ReservationException) ex).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_RESERVATION_STATUS_TRANSITION));
        }

        @Test
        @DisplayName("[실패] CANCEL_REQUESTED 예약 재취소 시도")
        void reservation_test_23_b() {
            // given — 이미 CANCEL_REQUESTED 상태
            reservation.confirm();
            reservation.requestCancel("불가피한 사유", CancelCategory.UNAVOIDABLE, CanceledBy.GUARDIAN);
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

            // when & then — isCancelable() 이 false 라 차단
            assertThatThrownBy(() -> reservationService.cancel(
                    member1Id, reservationId, personalCancelRequest))
                    .isInstanceOf(ReservationException.class)
                    .satisfies(ex -> assertThat(((ReservationException) ex).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_RESERVATION_STATUS_TRANSITION));
        }

        @Test
        @DisplayName("[실패] 당사자가 아닌 회원이 취소 시도")
        void reservation_test_24() {
            // given
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

            // when & then
            assertThatThrownBy(() -> reservationService.cancel(
                    member3Id, reservationId, personalCancelRequest))
                    .isInstanceOf(ReservationException.class)
                    .satisfies(ex -> assertThat(((ReservationException) ex).getErrorCode())
                            .isEqualTo(ReservationErrorCode.NOT_RESERVATION_PARTY));
        }
    }

    // 예약 만료 (expireOne) 단일 처리 테스트는 ReservationExpireServiceTest 로 이동
}
