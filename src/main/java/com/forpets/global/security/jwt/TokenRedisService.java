package com.forpets.global.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis를 사용해 Refresh Token과 Access Token 블랙리스트를 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRedisService {

    private static final String REFRESH_TOKEN_PREFIX = "jwt:refresh:";
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;

    public void saveRefreshToken(Long memberId, String refreshToken, long ttlMillis) {
        stringRedisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + memberId,
                refreshToken,
                Duration.ofMillis(ttlMillis)
        );
    }

    public String getRefreshToken(Long memberId) {
        return stringRedisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + memberId);
    }

    public void deleteRefreshToken(Long memberId) {
        stringRedisTemplate.delete(REFRESH_TOKEN_PREFIX + memberId);
    }

    public void addToBlacklist(String accessToken, long ttlMillis) {
        if (ttlMillis <= 0) {
            return;
        }

        stringRedisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + accessToken,
                "logout",
                Duration.ofMillis(ttlMillis)
        );
    }

    public boolean isBlacklisted(String accessToken) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + accessToken));
        } catch (Exception e) {
            log.warn("JWT 블랙리스트 Redis 조회 실패 — 보안 우선 원칙 적용, 토큰 차단 처리", e);
            return true; // 보안 우선: 조회 실패 시 차단
        }
    }
}