package com.forpets.domain.settlement.service;

import com.forpets.domain.member.entity.MemberRole;
import com.forpets.domain.settlement.dto.SettlementResponseDto;
import com.forpets.domain.settlement.entity.Settlement;
import com.forpets.domain.settlement.entity.SettlementStatus;
import com.forpets.domain.settlement.entity.SettlementType;
import com.forpets.domain.settlement.exception.SettlementErrorCode;
import com.forpets.domain.settlement.exception.SettlementException;
import com.forpets.domain.settlement.repository.SettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @InjectMocks
    private SettlementService settlementService;

    @Mock
    private SettlementRepository settlementRepository;

    private final Long reservationId = 1L;
    private final Long receiverMemberId = 2L;
    private final Long otherMemberId = 3L;
    private final Long sourcePaymentId = 10L;

    private Settlement settlement;

    @BeforeEach
    void setUp() {
        settlement = Settlement.builder()
                .reservationId(reservationId)
                .receiverMemberId(receiverMemberId)
                .sourcePaymentId(sourcePaymentId)
                .settlementType(SettlementType.CARE_COMPLETION)
                .originalAmount(100_000L)
                .platformFeeRate(10)
                .platformFeeAmount(10_000L)
                .settlementAmount(90_000L)
                .reason("케어 완료 정산")
                .build();
        ReflectionTestUtils.setField(settlement, "id", 100L);
    }

    @Nested
    @DisplayName("케어 완료 정산 생성")
    class CreateCareCompletionSettlementTest {

        @Test
        @DisplayName("[성공] 기본 플랫폼 수수료 10%를 차감한 READY 정산 생성")
        void settlement_test_01() {
            // given
            given(settlementRepository.existsByReservationId(reservationId)).willReturn(false);
            given(settlementRepository.save(any(Settlement.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            SettlementResponseDto result = settlementService.createCareCompletionSettlement(
                    reservationId, receiverMemberId, sourcePaymentId, 100_000L);

            // then
            assertThat(result.settlementType()).isEqualTo(SettlementType.CARE_COMPLETION);
            assertThat(result.status()).isEqualTo(SettlementStatus.READY);
            assertThat(result.originalAmount()).isEqualTo(100_000L);
            assertThat(result.platformFeeRate()).isEqualTo(10);
            assertThat(result.platformFeeAmount()).isEqualTo(10_000L);
            assertThat(result.settlementAmount()).isEqualTo(90_000L);
        }

        @Test
        @DisplayName("[실패] 같은 예약에 정산이 이미 있으면 중복 생성 차단")
        void settlement_test_02() {
            // given
            given(settlementRepository.existsByReservationId(reservationId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> settlementService.createCareCompletionSettlement(
                    reservationId, receiverMemberId, sourcePaymentId, 100_000L))
                    .isInstanceOf(SettlementException.class)
                    .satisfies(ex -> assertThat(((SettlementException) ex).getErrorCode())
                            .isEqualTo(SettlementErrorCode.SETTLEMENT_ALREADY_EXISTS));
        }
    }

    @Nested
    @DisplayName("위약금 보상 정산 생성")
    class CreatePenaltySettlementTest {

        @Test
        @DisplayName("[성공] 위약금 정산은 수수료 없이 보상금 전액을 정산금으로 생성")
        void settlement_test_03() {
            // given
            given(settlementRepository.existsByReservationId(reservationId)).willReturn(false);
            given(settlementRepository.save(any(Settlement.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            SettlementResponseDto result = settlementService.createPenaltySettlement(
                    reservationId,
                    receiverMemberId,
                    sourcePaymentId,
                    20_000L,
                    SettlementType.OWNER_CANCEL_PENALTY,
                    "보호자 귀책 취소 보상");

            // then
            assertThat(result.settlementType()).isEqualTo(SettlementType.OWNER_CANCEL_PENALTY);
            assertThat(result.platformFeeRate()).isZero();
            assertThat(result.platformFeeAmount()).isZero();
            assertThat(result.settlementAmount()).isEqualTo(20_000L);
        }

        @Test
        @DisplayName("[실패] 케어 완료 유형으로 위약금 정산 생성 불가")
        void settlement_test_04() {
            // when & then
            assertThatThrownBy(() -> settlementService.createPenaltySettlement(
                    reservationId,
                    receiverMemberId,
                    sourcePaymentId,
                    20_000L,
                    SettlementType.CARE_COMPLETION,
                    "잘못된 유형"))
                    .isInstanceOf(SettlementException.class)
                    .satisfies(ex -> assertThat(((SettlementException) ex).getErrorCode())
                            .isEqualTo(SettlementErrorCode.INVALID_SETTLEMENT_TYPE));
        }
    }

    @Nested
    @DisplayName("정산 조회")
    class GetSettlementTest {

        @Test
        @DisplayName("[성공] 수령자는 정산 상세 조회 가능")
        void settlement_test_05() {
            // given
            given(settlementRepository.findById(100L)).willReturn(Optional.of(settlement));

            // when
            SettlementResponseDto result = settlementService.getDetail(
                    receiverMemberId, MemberRole.SITTER, 100L);

            // then
            assertThat(result.settlementId()).isEqualTo(100L);
            assertThat(result.receiverMemberId()).isEqualTo(receiverMemberId);
        }

        @Test
        @DisplayName("[성공] ADMIN은 수령자가 아니어도 정산 상세 조회 가능")
        void settlement_test_06() {
            // given
            given(settlementRepository.findById(100L)).willReturn(Optional.of(settlement));

            // when
            SettlementResponseDto result = settlementService.getDetail(
                    otherMemberId, MemberRole.ADMIN, 100L);

            // then
            assertThat(result.settlementId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("[실패] 수령자와 ADMIN이 아니면 정산 상세 조회 불가")
        void settlement_test_07() {
            // given
            given(settlementRepository.findById(100L)).willReturn(Optional.of(settlement));

            // when & then
            assertThatThrownBy(() -> settlementService.getDetail(
                    otherMemberId, MemberRole.MEMBER, 100L))
                    .isInstanceOf(SettlementException.class)
                    .satisfies(ex -> assertThat(((SettlementException) ex).getErrorCode())
                            .isEqualTo(SettlementErrorCode.NOT_SETTLEMENT_RECEIVER));
        }

        @Test
        @DisplayName("[성공] 내 정산 목록 최신순 조회")
        void settlement_test_08() {
            // given
            given(settlementRepository.findAllByReceiverMemberIdOrderByCreatedAtDesc(receiverMemberId))
                    .willReturn(List.of(settlement));

            // when
            List<SettlementResponseDto> result = settlementService.getMySettlements(receiverMemberId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).receiverMemberId()).isEqualTo(receiverMemberId);
        }
    }
}
