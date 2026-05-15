package com.forpets.global.security.dto;

import com.forpets.domain.member.entity.MemberRole;

/**
 * JWT 인증이 완료된 회원의 최소 정보를 담습니다.
 */
public record CurrentMember(
        Long id,
        MemberRole role
) {
}