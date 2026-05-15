package com.forpets.domain.auth.dto;

import com.forpets.domain.member.entity.*;

import java.time.LocalDateTime;

/**
 * 회원가입 성공 응답 DTO입니다.
 */
public record SignUpResponse(
        Long id,
        String email,
        String nickname,
        MemberRole role,
        MemberStatus status,
        LocalDateTime createdAt
) {

    public static SignUpResponse from(Member member) {
        return new SignUpResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole(),
                member.getStatus(),
                member.getCreatedAt()
        );
    }
}