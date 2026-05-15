package com.forpets.domain.member.dto;

import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.MemberRole;
import com.forpets.domain.member.entity.MemberStatus;

import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String email,
        String nickname,
        String phone,
        MemberGender gender,
        MemberRole role,
        MemberStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getPhone(),
                member.getGender(),
                member.getRole(),
                member.getStatus(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
