package com.forpets.domain.sitter.dto.profile;

import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.sitter.dto.schedule.ScheduleResponseDto;
import com.forpets.domain.sitter.entity.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SitterResponseDto(
        Long id,
        Long memberId,
        Region region,
        String nickname,
        MemberGender gender,
        String introduction,
        Integer experienceYears,
        PossiblePetType possiblePetType,
        PossiblePetSize possiblePetSize,
        Integer pricePerHour,
        BigDecimal averageRating,
        Integer reviewCount,
        SitterProfileStatus status,
        SitterApprovalStatus approvalStatus,
        String rejectReason,
        List<ScheduleResponseDto> schedules,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SitterResponseDto from(SitterProfile sitter, Region region, String nickname, MemberGender gender) {
        return from(sitter, region, nickname, gender, List.of());
    }

    public static SitterResponseDto from(SitterProfile sitter, Region region, String nickname, MemberGender gender, List<SitterSchedule> schedules) {
        return new SitterResponseDto(
                sitter.getId(),
                sitter.getMemberId(),
                region,
                nickname,
                gender,
                sitter.getIntroduction(),
                sitter.getExperienceYears(),
                sitter.getPossiblePetType(),
                sitter.getPossiblePetSize(),
                sitter.getPricePerHour(),
                sitter.getAverageRating(),
                sitter.getReviewCount(),
                sitter.getStatus(),
                sitter.getApprovalStatus(),
                sitter.getRejectReason(),
                schedules.stream().map(ScheduleResponseDto::from).toList(),
                sitter.getCreatedAt(),
                sitter.getUpdatedAt()
        );
    }
}