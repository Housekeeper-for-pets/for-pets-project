package com.forpets.domain.settlement.entity;

import com.forpets.domain.settlement.exception.SettlementErrorCode;
import com.forpets.domain.settlement.exception.SettlementException;
import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "settlements", uniqueConstraints = {
        @UniqueConstraint(name = "uk_settlement_reservation", columnNames = "reservation_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "receiver_member_id", nullable = false)
    private Long receiverMemberId;

    @Column(name = "source_payment_id")
    private Long sourcePaymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_type", nullable = false, length = 30)
    private SettlementType settlementType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "original_amount", nullable = false)
    private Long originalAmount;

    @Column(name = "platform_fee_rate", nullable = false)
    private int platformFeeRate;

    @Column(name = "platform_fee_amount", nullable = false)
    private Long platformFeeAmount;

    @Column(name = "settlement_amount", nullable = false)
    private Long settlementAmount;

    @Column(length = 500)
    private String reason;

    @Column(name = "hold_reason", length = 500)
    private String holdReason;

    @Column(name = "failed_reason", length = 500)
    private String failedReason;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Builder
    private Settlement(Long reservationId, Long receiverMemberId, Long sourcePaymentId,
                       SettlementType settlementType, Long originalAmount, int platformFeeRate,
                       Long platformFeeAmount, Long settlementAmount, String reason) {
        this.reservationId = reservationId;
        this.receiverMemberId = receiverMemberId;
        this.sourcePaymentId = sourcePaymentId;
        this.settlementType = settlementType;
        this.originalAmount = originalAmount;
        this.platformFeeRate = platformFeeRate;
        this.platformFeeAmount = platformFeeAmount;
        this.settlementAmount = settlementAmount;
        this.reason = reason;
        this.status = SettlementStatus.READY;
        this.requestedAt = LocalDateTime.now();
    }

    public void hold(String holdReason) {
        validateNotFinal();
        this.status = SettlementStatus.HOLD;
        this.holdReason = holdReason;
    }

    public void approve() {
        validateReadyOrHold();
        this.status = SettlementStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.holdReason = null;
    }

    public void startProcessing() {
        if (this.status != SettlementStatus.APPROVED) {
            throw new SettlementException(SettlementErrorCode.INVALID_SETTLEMENT_STATUS);
        }
        this.status = SettlementStatus.PROCESSING;
        this.processedAt = LocalDateTime.now();
    }

    public void complete() {
        if (this.status != SettlementStatus.PROCESSING) {
            throw new SettlementException(SettlementErrorCode.INVALID_SETTLEMENT_STATUS);
        }
        this.status = SettlementStatus.COMPLETED;
        this.settledAt = LocalDateTime.now();
    }

    public void fail(String failedReason) {
        if (this.status != SettlementStatus.PROCESSING) {
            throw new SettlementException(SettlementErrorCode.INVALID_SETTLEMENT_STATUS);
        }
        this.status = SettlementStatus.FAILED;
        this.failedReason = failedReason;
    }

    public void cancel() {
        validateNotFinal();
        this.status = SettlementStatus.CANCELED;
    }

    private void validateReadyOrHold() {
        if (this.status != SettlementStatus.READY && this.status != SettlementStatus.HOLD) {
            throw new SettlementException(SettlementErrorCode.INVALID_SETTLEMENT_STATUS);
        }
    }

    private void validateNotFinal() {
        if (this.status == SettlementStatus.COMPLETED
                || this.status == SettlementStatus.FAILED
                || this.status == SettlementStatus.CANCELED) {
            throw new SettlementException(SettlementErrorCode.INVALID_SETTLEMENT_STATUS);
        }
    }
}
