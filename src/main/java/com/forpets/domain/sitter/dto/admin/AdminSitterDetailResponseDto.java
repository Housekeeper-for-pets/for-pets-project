package com.forpets.domain.sitter.dto.admin;

import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterApprovalStatus;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.entity.SitterProfileStatus;

import java.time.LocalDateTime;

/**
 * 관리자가 시터 승인 전 클릭 시 보여줄 시터 프로필 + 멤버 정보 통합 응답.
 * 시터 프로필 기본 정보 + 신청자(Member) 기본 정보를 함께 반환한다.
 */
public record AdminSitterDetailResponseDto(
        // ===== Sitter Profile =====
        Long sitterProfileId,
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
        LocalDateTime profileCreatedAt,
        LocalDateTime profileUpdatedAt,

        // ===== Member (신청자) =====
        Long memberId,
        String email,
        String nickname,
        String phone,
        Region region,
        MemberGender gender,
        LocalDateTime memberCreatedAt
) {
    public static AdminSitterDetailResponseDto from(SitterProfile sitter, Member member) {
        return new AdminSitterDetailResponseDto(
                sitter.getId(),
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
                sitter.getUpdatedAt(),
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getPhone(),
                member.getRegion(),
                member.getGender(),
                member.getCreatedAt()
        );
    }
}
