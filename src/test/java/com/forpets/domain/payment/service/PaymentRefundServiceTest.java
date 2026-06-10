package com.forpets.domain.payment.service;

import com.forpets.domain.coupon.service.CouponService;
import com.forpets.domain.payment.client.PortOneCancelResult;
import com.forpets.domain.payment.client.PortOnePaymentClient;
import com.forpets.domain.payment.entity.Payment;
import com.forpets.domain.payment.entity.PaymentProvider;
import com.forpets.domain.payment.entity.PaymentRole;
import com.forpets.domain.payment.entity.PaymentStatus;
import com.forpets.domain.payment.entity.PaymentType;
import com.forpets.domain.payment.repository.PaymentRepository;
import com.forpets.domain.reservation.entity.CanceledBy;
import com.forpets.domain.reservation.entity.ReservationPayment;
import com.forpets.domain.reservation.repository.ReservationPaymentRepository;
import com.forpets.domain.reservation.service.ReservationLockService;
import com.forpets.domain.settlement.entity.SettlementType;
import com.forpets.domain.settlement.service.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PaymentRefundServiceTest {

    @InjectMocks
    private PaymentRefundService paymentRefundService;

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ReservationPaymentRepository reservationPaymentRepository;
    @Mock
    private PortOnePaymentClient portOnePaymentClient;
    @Mock
    private CouponService couponService;
    @Mock
    private ReservationLockService reservationLockService;
    @Mock
    private SettlementService settlementService;

    // ── 픽스처 ──
    private final Long reservationId = 600L;
    private final Long guardianMemberId = 1L;
    private final Long sitterMemberId = 2L;

    // 보호자 10만원, 시터 2만원 (보호자 결제금의 20%)
    private static final long GUARDIAN_PAYMENT_AMOUNT = 100_000L;
    private static final long SITTER_PAYMENT_AMOUNT = 20_000L;

    // 임시 wallet — 시나리오 검증용
    private long guardianWallet;
    private long sitterWallet;
    private long platformHeldGuardian; // 플랫폼이 잡고 있던 보호자 결제금
    private long platformHeldSitter;   // 플랫폼이 잡고 있던 시터 예약금

    private Payment guardianPayment;
    private Payment sitterPayment;
    private ReservationPayment reservationPayment;

    @BeforeEach
    void setUp() {
        // 초기 wallet 상태: 양쪽 다 결제 완료된 시점
        // → 보호자: 결제로 10만 빠짐 (wallet=0), 플랫폼이 10만 보관 중
        // → 시터: 예약금 2만 빠짐 (wallet=0), 플랫폼이 2만 보관 중
        guardianWallet = 0L;
        sitterWallet = 0L;
        platformHeldGuardian = GUARDIAN_PAYMENT_AMOUNT;
        platformHeldSitter = SITTER_PAYMENT_AMOUNT;

        guardianPayment = Payment.builder()
                .reservationId(reservationId)
                .memberId(guardianMemberId)
                .paymentRole(PaymentRole.GUARDIAN)
                .paymentType(PaymentType.FULL)
                .originalAmount(GUARDIAN_PAYMENT_AMOUNT)
                .discountAmount(0L)
                .finalAmount(GUARDIAN_PAYMENT_AMOUNT)
                .userCouponId(null)
                .provider(PaymentProvider.PORTONE)
                .merchantUid("merchant-guardian-001")
                .build();
        ReflectionTestUtils.setField(guardianPayment, "id", 101L);
        ReflectionTestUtils.setField(guardianPayment, "status", PaymentStatus.PAID);

        sitterPayment = Payment.builder()
                .reservationId(reservationId)
                .memberId(sitterMemberId)
                .paymentRole(PaymentRole.SITTER)
                .paymentType(PaymentType.DEPOSIT)
                .originalAmount(SITTER_PAYMENT_AMOUNT)
                .discountAmount(0L)
                .finalAmount(SITTER_PAYMENT_AMOUNT)
                .userCouponId(null)
                .provider(PaymentProvider.PORTONE)
                .merchantUid("merchant-sitter-001")
                .build();
        ReflectionTestUtils.setField(sitterPayment, "id", 102L);
        ReflectionTestUtils.setField(sitterPayment, "status", PaymentStatus.PAID);

        reservationPayment = ReservationPayment.create(
                reservationId, (int) GUARDIAN_PAYMENT_AMOUNT, (int) SITTER_PAYMENT_AMOUNT);
        ReflectionTestUtils.setField(reservationPayment, "id", 1L);
        reservationPayment.guardianConfirm();
        reservationPayment.sitterConfirm();
    }

    /**
     * PG 환불 stub — 환불 금액만큼 플랫폼에서 사용자 wallet 으로 이동시킴
     */
    private void stubPgRefund(String merchantUid, PaymentRole role) {
        given(portOnePaymentClient.cancelPayment(eq(merchantUid), any(Long.class), anyString()))
                .willAnswer(invocation -> {
                    Long amount = invocation.getArgument(1);
                    if (role == PaymentRole.GUARDIAN) {
                        platformHeldGuardian -= amount;
                        guardianWallet += amount;
                    } else {
                        platformHeldSitter -= amount;
                        sitterWallet += amount;
                    }
                    return new PortOneCancelResult("{\"status\":\"CANCELED\"}");
                });
    }

    /**
     * 보호자 환불 Settlement 시뮬레이션 (관리자 수동 처리 결과)
     * 새 정책: 보호자 일방 취소 시 보호자 결제는 PG 호출 없이 Settlement 로 환불 처리
     */
    private void settleGuardianRefund(long refundAmount) {
        if (refundAmount <= 0) return;
        platformHeldGuardian -= refundAmount;
        guardianWallet += refundAmount;
    }

    /**
     * 위약금 정산 시뮬레이션
     * Settlement 도메인이 아직 없으므로 테스트에서 직접 wallet 이동 처리
     */
    private void settlePenalty(long penaltyAmount, CanceledBy canceledBy) {
        if (penaltyAmount <= 0) return;

        if (canceledBy == CanceledBy.GUARDIAN) {
            // 보호자가 취소 → 보호자 위약금이 시터에게 보상금으로
            platformHeldGuardian -= penaltyAmount;
            sitterWallet += penaltyAmount;
        } else {
            // 시터가 취소 → 시터 위약금이 보호자에게 보상금으로
            platformHeldSitter -= penaltyAmount;
            guardianWallet += penaltyAmount;
        }
    }

    @Nested
    @DisplayName("위약금 환불 — refundWithPenalty()")
    class RefundWithPenaltyTest {

        @Test
        @DisplayName("[시나리오 2] 보호자 PERSONAL 취소 → 보호자 결제는 Settlement 수동처리, 시터 예약금만 즉시 PG 환불")
        void scenario_2_guardian_personal_cancel() {
            /*
            정책 변경: PG 부분환불 502 이슈 우회를 위해
              - 보호자 결제(10만) → PG 호출 X, Settlement 2건 생성
                · GUARDIAN_PARTIAL_REFUND 8만 (관리자 수동 환불)
                · OWNER_CANCEL_PENALTY 2만 (시터에게 위약금 보상)
              - 시터 예약금(2만) → 시터 무귀책이므로 즉시 PG 전액 환불
             */
            // given
            given(paymentRepository.findAllByReservationIdAndStatusIn(
                    reservationId, List.of(PaymentStatus.PAID)))
                    .willReturn(List.of(guardianPayment, sitterPayment));
            given(reservationPaymentRepository.findByReservationId(reservationId))
                    .willReturn(Optional.of(reservationPayment));

            // 시터 결제만 PG 환불 stub (보호자는 PG 호출 안 됨)
            stubPgRefund("merchant-sitter-001", PaymentRole.SITTER);

            // when — 보호자가 취소
            paymentRefundService.refundWithPenalty(
                    reservationId, "개인 사정", CanceledBy.GUARDIAN);

            // 위약금/환불분 Settlement 시뮬레이션 (실제로는 관리자 수동 처리)
            long guardianPenalty = GUARDIAN_PAYMENT_AMOUNT * 20 / 100; // 2만
            long guardianRefundAmount = GUARDIAN_PAYMENT_AMOUNT - guardianPenalty; // 8만
            settlePenalty(guardianPenalty, CanceledBy.GUARDIAN);
            settleGuardianRefund(guardianRefundAmount);

            // then ── wallet 검증 ──
            assertThat(guardianWallet)
                    .as("보호자는 위약금 2만원 빼고 8만원 환불받음 (Settlement 수동 처리)")
                    .isEqualTo(80_000L);
            assertThat(sitterWallet)
                    .as("시터는 자기 예약금 2만원 (PG 환불) + 위약금 보상 2만원 = 4만원")
                    .isEqualTo(40_000L);
            assertThat(platformHeldGuardian).as("보호자 보관금 모두 정리").isEqualTo(0L);
            assertThat(platformHeldSitter).as("시터 보관금 모두 정리").isEqualTo(0L);

            // 결제 상태 검증
            assertThat(guardianPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(sitterPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

            // PG 호출 검증 — 보호자는 호출 안 되고 시터만 전액 환불
            then(portOnePaymentClient).should(never())
                    .cancelPayment(eq("merchant-guardian-001"), any(), anyString());
            then(portOnePaymentClient).should()
                    .cancelPayment(eq("merchant-sitter-001"), eq(20_000L), anyString());

            // Settlement 호출 검증 — 보호자 환불분 + 위약금 보상 각각 생성
            then(settlementService).should().createGuardianRefundSettlement(
                    eq(reservationId),
                    eq(guardianMemberId),
                    eq(guardianPayment.getId()),
                    eq(80_000L),
                    anyString()
            );
            then(settlementService).should().createPenaltySettlement(
                    reservationId,
                    sitterMemberId,
                    guardianPayment.getId(),
                    20_000L,
                    SettlementType.OWNER_CANCEL_PENALTY,
                    "보호자 귀책 취소 보상 - 개인 사정"
            );
        }

        @Test
        @DisplayName("[시나리오 3] 시터 PERSONAL 취소 → 보호자 12만원, 시터 0원")
        void scenario_3_sitter_personal_cancel() {
            // given
            given(paymentRepository.findAllByReservationIdAndStatusIn(
                    reservationId, List.of(PaymentStatus.PAID)))
                    .willReturn(List.of(guardianPayment, sitterPayment));
            given(reservationPaymentRepository.findByReservationId(reservationId))
                    .willReturn(Optional.of(reservationPayment));

            stubPgRefund("merchant-guardian-001", PaymentRole.GUARDIAN);
            // 시터 결제는 PG 환불 0원이라 stub 불필요 (cancelPayment 호출 안 됨)

            // when — 시터가 취소
            paymentRefundService.refundWithPenalty(
                    reservationId, "개인 사정", CanceledBy.SITTER);

            // 위약금 정산 (시터 예약금 전액 = 2만이 보호자에게)
            long sitterPenalty = SITTER_PAYMENT_AMOUNT;
            settlePenalty(sitterPenalty, CanceledBy.SITTER);

            // then ── wallet 검증 ──
            assertThat(guardianWallet)
                    .as("보호자는 결제금 10만 + 보상금 2만 = 12만원")
                    .isEqualTo(120_000L);
            assertThat(sitterWallet)
                    .as("시터는 예약금 전액 위약금이라 0원")
                    .isEqualTo(0L);
            assertThat(platformHeldGuardian).isEqualTo(0L);
            assertThat(platformHeldSitter).isEqualTo(0L);

            // 결제 상태 검증
            assertThat(guardianPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(sitterPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

            // PG 호출 검증
            then(portOnePaymentClient).should()
                    .cancelPayment(eq("merchant-guardian-001"), eq(100_000L), anyString());
            // 시터 결제는 전액 위약금이라 PG 호출 안 됨
            then(portOnePaymentClient).should(never())
                    .cancelPayment(eq("merchant-sitter-001"), any(), anyString());

            then(settlementService).should().createPenaltySettlement(
                    reservationId,
                    guardianMemberId,
                    sitterPayment.getId(),
                    20_000L,
                    SettlementType.SITTER_CANCEL_PENALTY,
                    "시터 귀책 취소 보상 - 개인 사정"
            );
        }
    }

    @Nested
    @DisplayName("전액 환불 — refundPaidPayments() (관리자 승인 시나리오 4)")
    class RefundPaidPaymentsTest {

        @Test
        @DisplayName("[시나리오 4-2] 관리자 승인 → 보호자 10만원, 시터 2만원 전액 환불")
        void scenario_4_admin_approve() {
            // given — 시터가 UNAVOIDABLE 로 요청 후 관리자가 승인한 상황
            given(paymentRepository.findAllByReservationIdAndStatusIn(
                    reservationId, List.of(PaymentStatus.PAID)))
                    .willReturn(List.of(guardianPayment, sitterPayment));
            given(reservationPaymentRepository.findByReservationId(reservationId))
                    .willReturn(Optional.of(reservationPayment));

            stubPgRefund("merchant-guardian-001", PaymentRole.GUARDIAN);
            stubPgRefund("merchant-sitter-001", PaymentRole.SITTER);

            // when — 전액 환불
            paymentRefundService.refundPaidPayments(reservationId, "불가피한 사유 승인");

            // then ── wallet 검증 ──
            assertThat(guardianWallet)
                    .as("관리자 승인이라 보호자 10만원 전액 환불")
                    .isEqualTo(100_000L);
            assertThat(sitterWallet)
                    .as("관리자 승인이라 시터도 예약금 2만원 전액 환불")
                    .isEqualTo(20_000L);
            assertThat(platformHeldGuardian).isEqualTo(0L);
            assertThat(platformHeldSitter).isEqualTo(0L);

            // 결제 상태 검증
            assertThat(guardianPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(sitterPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

            // PG 호출 — 양쪽 다 전액 환불
            then(portOnePaymentClient).should()
                    .cancelPayment(eq("merchant-guardian-001"), eq(100_000L), anyString());
            then(portOnePaymentClient).should()
                    .cancelPayment(eq("merchant-sitter-001"), eq(20_000L), anyString());
        }
    }

    @Nested
    @DisplayName("케어 완료 예약금 환불 — refundSitterDepositAfterCompletion()")
    class RefundSitterDepositAfterCompletionTest {

        @Test
        @DisplayName("[성공] 정상 케어 완료 후 시터 예약금만 환불")
        void refund_sitter_deposit_after_completion() {
            // given
            given(paymentRepository.findByReservationIdAndPaymentRoleAndStatus(
                    reservationId, PaymentRole.SITTER, PaymentStatus.PAID))
                    .willReturn(Optional.of(sitterPayment));
            given(reservationPaymentRepository.findByReservationId(reservationId))
                    .willReturn(Optional.of(reservationPayment));

            stubPgRefund("merchant-sitter-001", PaymentRole.SITTER);

            // when
            paymentRefundService.refundSitterDepositAfterCompletion(reservationId);

            // then
            assertThat(guardianWallet).as("보호자 결제금은 정산 원천이라 환불하지 않음").isEqualTo(0L);
            assertThat(sitterWallet).as("시터 예약금만 환불").isEqualTo(20_000L);
            assertThat(platformHeldGuardian).isEqualTo(100_000L);
            assertThat(platformHeldSitter).isEqualTo(0L);
            assertThat(guardianPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(sitterPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(reservationPayment.isGuardianPaid()).isTrue();
            assertThat(reservationPayment.isSitterPaid()).isFalse();

            then(portOnePaymentClient).should()
                    .cancelPayment(eq("merchant-sitter-001"), eq(20_000L), anyString());
            then(portOnePaymentClient).should(never())
                    .cancelPayment(eq("merchant-guardian-001"), any(), anyString());
        }
    }
}
