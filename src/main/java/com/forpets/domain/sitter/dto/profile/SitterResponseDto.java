package com.forpets.domain.sitter.dto.profile;

import com.forpets.domain.member.entity.Region;
import com.forpets.domain.sitter.dto.schedule.ScheduleResponseDto;
import com.forpets.domain.sitter.entity.*;

import java.time.LocalDateTime;
import java.util.List;

public record SitterResponseDto(
        Long id,
        Long memberId,
        Region region,
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
    public static SitterResponseDto from(SitterProfile sitter, Region region) {
        return from(sitter, region, List.of());
    }

    public static SitterResponseDto from(SitterProfile sitter, Region region, List<SitterSchedule> schedules) {
        return new SitterResponseDto(
                sitter.getId(),
                sitter.getMemberId(),
                region,
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