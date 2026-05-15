package com.forpets.global.security.jwt;

import com.forpets.domain.member.entity.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Access Token과 Refresh Token 생성, 검증, Claim 추출을 담당합니다.
 */
@Getter
@Component
public class JwtTokenProvider {

    private static final String ISSUER = "ForPetsAuthServer";
    private static final String ROLE_CLAIM = "role";

    @Value("${jwt.secret-key}")
    private String secretKeyString;

    @Value("${jwt.access-token-validity-time}")
    private long accessTokenValidityTime;

    @Value("${jwt.refresh-token-validity-time}")
    private long refreshTokenValidityTime;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret-key는 최소 32바이트 이상이어야 합니다.");
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long memberId, MemberRole role) {
        return createToken(memberId, role, accessTokenValidityTime);
    }

    public String createRefreshToken(Long memberId) {
        return createToken(memberId, null, refreshTokenValidityTime);
    }

    private String createToken(Long memberId, MemberRole role, long validityTime) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + validityTime);

        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuer(ISSUER)
                .issuedAt(now)
                .expiration(expiration)
                .id(UUID.randomUUID().toString())
                .signWith(secretKey);

        if (role != null) {
            builder.claim(ROLE_CLAIM, role.name());
        }

        return builder.compact();
    }

    public boolean validateToken(String token) {
        parseClaims(token);
        return true;
    }

    public Long getMemberId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public MemberRole getRole(String token) {
        String role = parseClaims(token).get(ROLE_CLAIM, String.class);
        return MemberRole.valueOf(role);
    }

    public long getRemainingTime(String token) {
        Date expiration = parseClaims(token).getExpiration();
        long remainingTime = expiration.getTime() - System.currentTimeMillis();
        return Math.max(remainingTime, 0);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}