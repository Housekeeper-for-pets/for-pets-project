package com.forpets.global.security;

/**
 * 인증 없이 접근 가능한 경로를 한 곳에서 관리합니다.
 * SecurityConfig와 JwtAuthFilter가 같은 기준을 사용하도록 분리했습니다.
 */
public final class PublicPathPatterns {

    public static final String[] ANY_METHOD = {
            "/health",
            "/actuator/**",
            "/error",
            "/favicon.ico",
            "/ws/**"
    };

    public static final String[] PUBLIC_POST = {
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/reissue",
            "/api/payments/webhook/portone"
    };

    public static final String[] PUBLIC_GET = {
            "/api/sitters",
//            "/api/sitters/*",
//            "/api/sitters/*/schedules",
            "/api/posts",
            "/api/posts/*"
    };

    public static final String[] AUTHENTICATED_GET = {
            "/api/posts/me"
    };

    private PublicPathPatterns() {
    }
}
