package com.forpets.domain.sitter.dto.admin;

import com.forpets.domain.member.entity.Region;
import com.forpets.domain.sitter.entity.*;

import java.time.LocalDateTime;

public record AdminSitterResponseDto(
        Long id,
        Long memberId,
        Region region,
        String introduction,
        Integer experienceYears,
        PossiblePetType possiblePetType,
        PossiblePetSize possiblePetSize,
        Integer pricePerHour,
        SitterProfileStatus status,
        SitterApprovalStatus approvalStatus,
        String rejectReason,
        Long evaluatedBy,
        LocalDateTime evaluatedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminSitterResponseDto from(SitterProfile sitter, Region region) {
        return new AdminSitterResponseDto(
                sitter.getId(),
                sitter.getMemberId(),
                region,
                sitter.getIntroduction(),
                sitter.getExperienceYears(),
                sitter.getPossiblePetType(),
                sitter.getPossiblePetSize(),
                sitter.getPricePerHour(),
                sitter.getStatus(),
                sitter.getApprovalStatus(),
                sitter.getRejectReason(),
                sitter.getEvaluatedBy(),
                sitter.getEvaluatedAt(),
                sitter.getCreatedAt(),
                sitter.getUpdatedAt()
        );
    }
}