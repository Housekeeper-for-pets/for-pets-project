package com.forpets.domain.ai.chat.dto;

import com.forpets.domain.member.entity.Region;
import com.forpets.domain.sitter.dto.schedule.ScheduleResponseDto;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterProfileStatus;

import java.util.List;

public record RecommendedSitterDto(
        Long sitterId,
        Long memberId,
        Region region,
        String introduction,
        Integer experienceYears,
        PossiblePetType possiblePetType,
        PossiblePetSize possiblePetSize,
        Integer pricePerHour,
        SitterProfileStatus status,
        String reviewSummary,
        List<String> strengths,
        List<String> cautions,
        List<ScheduleResponseDto> schedules
) {
}
