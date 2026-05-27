package com.forpets.domain.settlement.service;

import com.forpets.domain.member.entity.MemberRole;
import com.forpets.domain.settlement.dto.SettlementResponseDto;
import com.forpets.domain.settlement.entity.Settlement;
import com.forpets.domain.settlement.entity.SettlementType;
import com.forpets.domain.settlement.exception.SettlementErrorCode;
import com.forpets.domain.settlement.exception.SettlementException;
import com.forpets.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    private static final int DEFAULT_PLATFORM_FEE_RATE = 10;
    private static final int PENALTY_PLATFORM_FEE_RATE = 0;

    private final SettlementRepository settlementRepository;

    public List<SettlementResponseDto> getMySettlements(Long memberId) {
        return settlementRepository.findAllByReceiverMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(SettlementResponseDto::from)
                .toList();
    }

    public SettlementResponseDto getDetail(Long memberId, MemberRole role, Long settlementId) {
        Settlement settlement = findById(settlementId);
        validateReadable(memberId, role, settlement);
        return SettlementResponseDto.from(settlement);
    }

    @Transactional
    public SettlementResponseDto createCareCompletionSettlement(Long reservationId,
                                                               Long sitterMemberId,
                                                               Long sourcePaymentId,
                                                               Long originalAmount) {
        validatePositiveAmount(originalAmount);
        validateNotExists(reservationId);

        return SettlementResponseDto.from(saveSettlement(
                reservationId,
                sitterMemberId,
                sourcePaymentId,
                SettlementType.CARE_COMPLETION,
                originalAmount,
                DEFAULT_PLATFORM_FEE_RATE,
                "케어 완료 정산"
        ));
    }

    @Transactional
    public SettlementResponseDto createPenaltySettlement(Long reservationId,
                                                        Long receiverMemberId,
                                                        Long sourcePaymentId,
                                                        Long penaltyAmount,
                                                        SettlementType settlementType,
                                                        String reason) {
        validatePenaltyType(settlementType);
        validatePositiveAmount(penaltyAmount);
        validateNotExists(reservationId);

        return SettlementResponseDto.from(saveSettlement(
                reservationId,
                receiverMemberId,
                sourcePaymentId,
                settlementType,
                penaltyAmount,
                PENALTY_PLATFORM_FEE_RATE,
                reason
        ));
    }

    private Settlement saveSettlement(Long reservationId,
                                      Long receiverMemberId,
                                      Long sourcePaymentId,
                                      SettlementType settlementType,
                                      Long originalAmount,
                                      int platformFeeRate,
                                      String reason) {
        Long platformFeeAmount = calculatePlatformFeeAmount(originalAmount, platformFeeRate);
        Long settlementAmount = calculateSettlementAmount(originalAmount, platformFeeAmount);

        Settlement settlement = Settlement.builder()
                .reservationId(reservationId)
                .receiverMemberId(receiverMemberId)
                .sourcePaymentId(sourcePaymentId)
                .settlementType(settlementType)
                .originalAmount(originalAmount)
                .platformFeeRate(platformFeeRate)
                .platformFeeAmount(platformFeeAmount)
                .settlementAmount(settlementAmount)
                .reason(reason)
                .build();

        return settlementRepository.save(settlement);
    }

    private Long calculatePlatformFeeAmount(Long amount, int platformFeeRate) {
        return amount * platformFeeRate / 100;
    }

    private Long calculateSettlementAmount(Long amount, Long platformFeeAmount) {
        return amount - platformFeeAmount;
    }

    private Settlement findById(Long settlementId) {
        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> new SettlementException(SettlementErrorCode.SETTLEMENT_NOT_FOUND));
    }

    private void validateReadable(Long memberId, MemberRole role, Settlement settlement) {
        if (role == MemberRole.ADMIN || settlement.getReceiverMemberId().equals(memberId)) {
            return;
        }
        throw new SettlementException(SettlementErrorCode.NOT_SETTLEMENT_RECEIVER);
    }

    private void validateNotExists(Long reservationId) {
        if (settlementRepository.existsByReservationId(reservationId)) {
            throw new SettlementException(SettlementErrorCode.SETTLEMENT_ALREADY_EXISTS);
        }
    }

    private void validatePositiveAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new SettlementException(SettlementErrorCode.INVALID_SETTLEMENT_AMOUNT);
        }
    }

    private void validatePenaltyType(SettlementType settlementType) {
        if (settlementType != SettlementType.SITTER_CANCEL_PENALTY
                && settlementType != SettlementType.OWNER_CANCEL_PENALTY) {
            throw new SettlementException(SettlementErrorCode.INVALID_SETTLEMENT_TYPE);
        }
    }
}
