package com.forpets.domain.settlement.dto;

import com.forpets.domain.settlement.entity.Settlement;
import com.forpets.domain.settlement.entity.SettlementStatus;
import com.forpets.domain.settlement.entity.SettlementType;

import java.time.LocalDateTime;

public record SettlementResponseDto(
        Long settlementId,
        Long reservationId,
        Long receiverMemberId,
        Long sourcePaymentId,
        SettlementType settlementType,
        SettlementStatus status,
        Long originalAmount,
        int platformFeeRate,
        Long platformFeeAmount,
        Long settlementAmount,
        String reason,
        String holdReason,
        String failedReason,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        LocalDateTime processedAt,
        LocalDateTime settledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SettlementResponseDto from(Settlement settlement) {
        return new SettlementResponseDto(
                settlement.getId(),
                settlement.getReservationId(),
                settlement.getReceiverMemberId(),
                settlement.getSourcePaymentId(),
                settlement.getSettlementType(),
                settlement.getStatus(),
                settlement.getOriginalAmount(),
                settlement.getPlatformFeeRate(),
                settlement.getPlatformFeeAmount(),
                settlement.getSettlementAmount(),
                settlement.getReason(),
                settlement.getHoldReason(),
                settlement.getFailedReason(),
                settlement.getRequestedAt(),
                settlement.getApprovedAt(),
                settlement.getProcessedAt(),
                settlement.getSettledAt(),
                settlement.getCreatedAt(),
                settlement.getUpdatedAt()
        );
    }
}
