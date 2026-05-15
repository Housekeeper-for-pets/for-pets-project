package com.forpets.global.common;

/**
 * 단순 성공 메시지를 내려줄 때 사용하는 공통 응답 DTO입니다.
 * 로그아웃, 회원 탈퇴, 삭제 성공 같은 응답에서 재사용합니다.
 */
public record MessageResponse(
        String message
) {

    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}