package com.forpets.domain.member.dto;

import com.forpets.domain.member.entity.*;

import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String email,
        String nickname,
        String phone,
        MemberGender gender,
        Region region,
        MemberRole role,
        MemberStatus status,
        int couponCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MemberResponse of(Member member, int couponCount) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getPhone(),
                member.getGender(),
                member.getRegion(),
                member.getRole(),
                member.getStatus(),
                couponCount,
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
