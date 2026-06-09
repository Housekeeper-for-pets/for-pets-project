package com.forpets.domain.auth.service;

import com.forpets.domain.auth.dto.*;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.exception.*;
import com.forpets.domain.member.repository.MemberRepository;
import com.forpets.global.exception.*;
import com.forpets.global.security.jwt.*;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입, 로그인, 로그아웃, 토큰 재발급 흐름을 처리합니다.
 * Refresh Token은 Redis에 저장하고, 재발급 시 Rotation 전략을 적용합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRedisService tokenRedisService;
    private final BearerTokenResolver bearerTokenResolver;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        validateSignUpDuplicate(request);

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .phone(request.phone())
                .gender(request.gender())
                .region(request.region())
                .build();

        Member savedMember = memberRepository.save(member);
        return SignUpResponse.from(savedMember);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new MemberException(MemberErrorCode.AUTHENTICATION_FAILED));

        validateLoginMember(member);

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new MemberException(MemberErrorCode.AUTHENTICATION_FAILED);
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        tokenRedisService.saveRefreshToken(
                member.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenValidityTime()
        );

        return TokenResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenValidityTime() / 1000
        );
    }

    @Transactional
    public TokenResponse reissue(ReissueRequest request) {
        String refreshToken = request.refreshToken();

        try {
            jwtTokenProvider.validateToken(refreshToken);
        } catch (RuntimeException exception) {
            throw new BusinessException(CommonErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        String savedRefreshToken = tokenRedisService.getRefreshToken(memberId);

        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(CommonErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        validateLoginMember(member);

        String newAccessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        tokenRedisService.saveRefreshToken(
                member.getId(),
                newRefreshToken,
                jwtTokenProvider.getRefreshTokenValidityTime()
        );

        return TokenResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtTokenProvider.getAccessTokenValidityTime() / 1000
        );
    }

    @Transactional
    public void logout(HttpServletRequest request) {
        String accessToken = bearerTokenResolver.resolve(request);
        if (accessToken == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        try {
            Long memberId = jwtTokenProvider.getMemberId(accessToken);
            long remainingTime = jwtTokenProvider.getRemainingTime(accessToken);
            tokenRedisService.addToBlacklist(accessToken, remainingTime);
            tokenRedisService.deleteRefreshToken(memberId);
        } catch (ExpiredJwtException e) {
            // 만료 토큰은 블랙리스트 등록 불필요, Refresh Token만 삭제
            Long memberId = Long.parseLong(e.getClaims().getSubject());
            tokenRedisService.deleteRefreshToken(memberId);
        }
    }

    private void validateSignUpDuplicate(SignUpRequest request) {
        if (memberRepository.countByEmailIncludingDeleted(request.email()) > 0) {
            throw new MemberException(MemberErrorCode.EMAIL_DUPLICATED);
        }

        if (memberRepository.countByNicknameIncludingDeleted(request.nickname()) > 0) {
            throw new MemberException(MemberErrorCode.NICKNAME_DUPLICATED);
        }
    }

    private void validateLoginMember(Member member) {
        if (member.isDeleted()) {
            throw new MemberException(MemberErrorCode.ACCOUNT_DELETED);
        }

        if (member.isSuspended()) {
            throw new MemberException(MemberErrorCode.ACCOUNT_SUSPENDED);
        }
    }
}