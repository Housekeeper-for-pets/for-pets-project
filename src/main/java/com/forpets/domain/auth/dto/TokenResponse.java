package com.forpets.domain.auth.dto;

/**
 * 로그인과 토큰 재발급 시 내려주는 토큰 응답 DTO입니다.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {

    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}