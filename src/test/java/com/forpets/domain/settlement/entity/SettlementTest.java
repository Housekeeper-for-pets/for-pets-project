package com.forpets.domain.settlement.entity;

import com.forpets.domain.settlement.exception.SettlementErrorCode;
import com.forpets.domain.settlement.exception.SettlementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementTest {

    @Test
    @DisplayName("[성공] READY 정산을 HOLD 처리")
    void hold_ready_settlement() {
        // given
        Settlement settlement = createSettlement();

        // when
        settlement.hold("계좌 확인 필요");

        // then
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.HOLD);
        assertThat(settlement.getHoldReason()).isEqualTo("계좌 확인 필요");
    }

    @Test
    @DisplayName("[성공] HOLD 정산 승인 시 보류 사유 제거")
    void approve_hold_settlement() {
        // given
        Settlement settlement = createSettlement();
        settlement.hold("계좌 확인 필요");

        // when
        settlement.approve();

        // then
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.APPROVED);
        assertThat(settlement.getHoldReason()).isNull();
        assertThat(settlement.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("[성공] APPROVED 정산을 PROCESSING으로 변경")
    void start_processing_approved_settlement() {
        // given
        Settlement settlement = createSettlement();
        settlement.approve();

        // when
        settlement.startProcessing();

        // then
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PROCESSING);
        assertThat(settlement.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("[성공] PROCESSING 정산 완료 처리")
    void complete_processing_settlement() {
        // given
        Settlement settlement = createProcessingSettlement();

        // when
        settlement.complete();

        // then
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(settlement.getSettledAt()).isNotNull();
    }

    @Test
    @DisplayName("[성공] PROCESSING 정산 실패 처리")
    void fail_processing_settlement() {
        // given
        Settlement settlement = createProcessingSettlement();

        // when
        settlement.fail("계좌 오류");

        // then
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.FAILED);
        assertThat(settlement.getFailedReason()).isEqualTo("계좌 오류");
    }

    @Test
    @DisplayName("[실패] READY 정산은 완료 처리 불가")
    void complete_ready_settlement_fails() {
        // given
        Settlement settlement = createSettlement();

        // when & then
        assertThatThrownBy(settlement::complete)
                .isInstanceOf(SettlementException.class)
                .satisfies(ex -> assertThat(((SettlementException) ex).getErrorCode())
                        .isEqualTo(SettlementErrorCode.INVALID_SETTLEMENT_STATUS));
    }

    @Test
    @DisplayName("[실패] 종료 상태 정산은 보류 처리 불가")
    void hold_final_settlement_fails() {
        // given
        Settlement settlement = createProcessingSettlement();
        settlement.complete();

        // when & then
        assertThatThrownBy(() -> settlement.hold("재확인"))
                .isInstanceOf(SettlementException.class)
                .satisfies(ex -> assertThat(((SettlementException) ex).getErrorCode())
                        .isEqualTo(SettlementErrorCode.INVALID_SETTLEMENT_STATUS));
    }

    private Settlement createProcessingSettlement() {
        Settlement settlement = createSettlement();
        settlement.approve();
        settlement.startProcessing();
        return settlement;
    }

    private Settlement createSettlement() {
        return Settlement.builder()
                .reservationId(1L)
                .receiverMemberId(2L)
                .sourcePaymentId(10L)
                .settlementType(SettlementType.CARE_COMPLETION)
                .originalAmount(100_000L)
                .platformFeeRate(10)
                .platformFeeAmount(10_000L)
                .settlementAmount(90_000L)
                .reason("케어 완료 정산")
                .build();
    }
}
