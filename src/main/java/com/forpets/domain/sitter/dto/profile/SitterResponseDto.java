package com.forpets.domain.sitter.dto.profile;

import com.forpets.domain.sitter.dto.schedule.ScheduleResponseDto;
import com.forpets.domain.sitter.entity.*;

import java.time.LocalDateTime;
import java.util.List;

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
        List<ScheduleResponseDto> schedules,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SitterResponseDto from(SitterProfile sitter) {
        return from(sitter, List.of());
    }

    public static SitterResponseDto from(SitterProfile sitter, List<SitterSchedule> schedules) {
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
                schedules.stream().map(ScheduleResponseDto::from).toList(),
                sitter.getCreatedAt(),
                sitter.getUpdatedAt()
        );
    }
}