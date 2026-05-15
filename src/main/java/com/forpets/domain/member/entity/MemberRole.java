package com.forpets.domain.member.entity;

import lombok.Getter;

/**
 * ForPets 회원 역할입니다.
 * 회원가입 기본 역할은 MEMBER이며, ADMIN은 일반 회원가입 API로 생성하지 않습니다.
 */
@Getter
public enum MemberRole {

    MEMBER("ROLE_MEMBER"),
    SITTER("ROLE_SITTER"),
    ADMIN("ROLE_ADMIN");

    private final String authority;

    MemberRole(String authority) {
        this.authority = authority;
    }
}