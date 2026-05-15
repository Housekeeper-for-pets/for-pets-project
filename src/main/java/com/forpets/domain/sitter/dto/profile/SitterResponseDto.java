package com.forpets.domain.sitter.dto.profile;

import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.entity.SitterProfileStatus;

import java.time.LocalDateTime;

public record SitterResponseDto(
        Long id,
        Long memberId,
        String region,
        String introduction,
        Integer experienceYears,
        PossiblePetType possiblePetType,
        PossiblePetSize possiblePetSize,
        Integer pricePerHour,
        SitterProfileStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SitterResponseDto from(SitterProfile sitter) {
        return new SitterResponseDto(
                sitter.getId(),
                sitter.getMemberId(),
                sitter.getRegion(),
                sitter.getIntroduction(),
                sitter.getExperienceYears(),
                sitter.getPossiblePetType(),
                sitter.getPossiblePetSize(),
                sitter.getPricePerHour(),
                sitter.getStatus(),
                sitter.getCreatedAt(),
                sitter.getUpdatedAt()
        );
    }
}