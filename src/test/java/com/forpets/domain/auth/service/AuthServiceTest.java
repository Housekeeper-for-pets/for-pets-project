package com.forpets.domain.auth.service;

import com.forpets.domain.auth.dto.LoginRequest;
import com.forpets.domain.auth.dto.ReissueRequest;
import com.forpets.domain.auth.dto.SignUpRequest;
import com.forpets.domain.auth.dto.SignUpResponse;
import com.forpets.domain.auth.dto.TokenResponse;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.MemberRole;
import com.forpets.domain.member.entity.MemberStatus;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.member.exception.MemberErrorCode;
import com.forpets.domain.member.exception.MemberException;
import com.forpets.domain.member.repository.MemberRepository;
import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.CommonErrorCode;
import com.forpets.global.security.jwt.BearerTokenResolver;
import com.forpets.global.security.jwt.JwtTokenProvider;
import com.forpets.global.security.jwt.TokenRedisService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private MemberRepository memberRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TokenRedisService tokenRedisService;
    @Mock private BearerTokenResolver bearerTokenResolver;

    private Member activeMember;
    private Member deletedMember;
    private Member suspendedMember;

    @BeforeEach
    void setUp() {
        activeMember = Member.builder()
                .email("user@example.com")
                .password("encodedPassword")
                .nickname("tester")
                .phone("010-1234-5678")
                .gender(MemberGender.MALE)
                .region(Region.GANGNAM)
                .build();
        ReflectionTestUtils.setField(activeMember, "id", 1L);

        deletedMember = Member.builder()
                .email("deleted@example.com")
                .password("encodedPassword")
                .nickname("deletedUser")
                .gender(MemberGender.UNKNOWN)
                .region(Region.UNKNOWN)
                .build();
        ReflectionTestUtils.setField(deletedMember, "id", 2L);
        deletedMember.delete();

        suspendedMember = Member.builder()
                .email("suspended@example.com")
                .password("encodedPassword")
                .nickname("suspendedUser")
                .gender(MemberGender.UNKNOWN)
                .region(Region.UNKNOWN)
                .build();
        ReflectionTestUtils.setField(suspendedMember, "id", 3L);
        ReflectionTestUtils.setField(suspendedMember, "status", MemberStatus.SUSPENDED);
    }

    // =============================================
    // signUp
    // =============================================
    @Nested
    @DisplayName("회원가입 — signUp()")
    class SignUpTest {

        private SignUpRequest request;

        @BeforeEach
        void setUp() {
            request = new SignUpRequest(
                    "new@example.com", "password123!", "newUser",
                    "010-0000-0000", MemberGender.MALE, Region.GANGNAM
            );
        }

        @Test
        @DisplayName("[성공] 정상 회원가입 시 저장된 회원 정보를 반환한다")
        void signUp_success() {
            doReturn(0).when(memberRepository).countByEmailIncludingDeleted("new@example.com");
            doReturn(0).when(memberRepository).countByNicknameIncludingDeleted("newUser");
            given(passwordEncoder.encode("password123!")).willReturn("encodedPw");
            given(memberRepository.save(any(Member.class))).willReturn(activeMember);

            SignUpResponse response = authService.signUp(request);

            assertThat(response).isNotNull();
            then(memberRepository).should().save(any(Member.class));
        }

        @Test
        @DisplayName("[실패] 이미 가입된 이메일이면 EMAIL_DUPLICATED 예외를 던진다")
        void signUp_emailDuplicated() {
            doReturn(1).when(memberRepository).countByEmailIncludingDeleted("new@example.com");

            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOf(MemberException.class)
                    .satisfies(ex -> assertThat(((MemberException) ex).getErrorCode())
                            .isEqualTo(MemberErrorCode.EMAIL_DUPLICATED));

            then(memberRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("[실패] 이미 사용 중인 닉네임이면 NICKNAME_DUPLICATED 예외를 던진다")
        void signUp_nicknameDuplicated() {
            doReturn(0).when(memberRepository).countByEmailIncludingDeleted("new@example.com");
            doReturn(1).when(memberRepository).countByNicknameIncludingDeleted("newUser");

            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOf(MemberException.class)
                    .satisfies(ex -> assertThat(((MemberException) ex).getErrorCode())
                            .isEqualTo(MemberErrorCode.NICKNAME_DUPLICATED));

            then(memberRepository).should(never()).save(any());
        }
    }

    // =============================================
    // login
    // =============================================
    @Nested
    @DisplayName("로그인 — login()")
    class LoginTest {

        private LoginRequest request;

        @BeforeEach
        void setUp() {
            request = new LoginRequest("user@example.com", "rawPassword");
        }

        @Test
        @DisplayName("[성공] 정상 로그인 시 AccessToken과 RefreshToken을 반환한다")
        void login_success() {
            given(memberRepository.findByEmail("user@example.com")).willReturn(Optional.of(activeMember));
            given(passwordEncoder.matches("rawPassword", "encodedPassword")).willReturn(true);
            given(jwtTokenProvider.createAccessToken(anyLong(), any(MemberRole.class))).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken(anyLong())).willReturn("refresh-token");
            given(jwtTokenProvider.getAccessTokenValidityTime()).willReturn(3600000L);
            given(jwtTokenProvider.getRefreshTokenValidityTime()).willReturn(604800000L);

            TokenResponse response = authService.login(request);

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            then(tokenRedisService).should().saveRefreshToken(anyLong(), anyString(), anyLong());
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 이메일이면 AUTHENTICATION_FAILED 예외를 던진다")
        void login_emailNotFound() {
            given(memberRepository.findByEmail("user@example.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(MemberException.class)
                    .satisfies(ex -> assertThat(((MemberException) ex).getErrorCode())
                            .isEqualTo(MemberErrorCode.AUTHENTICATION_FAILED));
        }

        @Test
        @DisplayName("[실패] 비밀번호가 불일치하면 AUTHENTICATION_FAILED 예외를 던진다")
        void login_passwordMismatch() {
            given(memberRepository.findByEmail("user@example.com")).willReturn(Optional.of(activeMember));
            given(passwordEncoder.matches("rawPassword", "encodedPassword")).willReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(MemberException.class)
                    .satisfies(ex -> assertThat(((MemberException) ex).getErrorCode())
                            .isEqualTo(MemberErrorCode.AUTHENTICATION_FAILED));
        }

        @Test
        @DisplayName("[실패] 탈퇴한 계정이면 ACCOUNT_DELETED 예외를 던진다")
        void login_accountDeleted() {
            given(memberRepository.findByEmail("user@example.com")).willReturn(Optional.of(deletedMember));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(MemberException.class)
                    .satisfies(ex -> assertThat(((MemberException) ex).getErrorCode())
                            .isEqualTo(MemberErrorCode.ACCOUNT_DELETED));
        }

        @Test
        @DisplayName("[실패] 정지된 계정이면 ACCOUNT_SUSPENDED 예외를 던진다")
        void login_accountSuspended() {
            given(memberRepository.findByEmail("user@example.com")).willReturn(Optional.of(suspendedMember));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(MemberException.class)
                    .satisfies(ex -> assertThat(((MemberException) ex).getErrorCode())
                            .isEqualTo(MemberErrorCode.ACCOUNT_SUSPENDED));
        }
    }

    // =============================================
    // reissue
    // =============================================
    @Nested
    @DisplayName("토큰 재발급 — reissue()")
    class ReissueTest {

        @Test
        @DisplayName("[성공] 유효한 RefreshToken으로 새 토큰 쌍을 반환한다")
        void reissue_success() {
            given(jwtTokenProvider.validateToken("valid-refresh")).willReturn(true);
            given(jwtTokenProvider.getMemberId("valid-refresh")).willReturn(1L);
            given(tokenRedisService.getRefreshToken(1L)).willReturn("valid-refresh");
            given(memberRepository.findById(1L)).willReturn(Optional.of(activeMember));
            given(jwtTokenProvider.createAccessToken(anyLong(), any(MemberRole.class))).willReturn("new-access");
            given(jwtTokenProvider.createRefreshToken(anyLong())).willReturn("new-refresh");
            given(jwtTokenProvider.getAccessTokenValidityTime()).willReturn(3600000L);
            given(jwtTokenProvider.getRefreshTokenValidityTime()).willReturn(604800000L);

            TokenResponse response = authService.reissue(new ReissueRequest("valid-refresh"));

            assertThat(response.accessToken()).isEqualTo("new-access");
            assertThat(response.refreshToken()).isEqualTo("new-refresh");
        }

        @Test
        @DisplayName("[실패] 유효하지 않은 RefreshToken이면 INVALID_REFRESH_TOKEN 예외를 던진다")
        void reissue_invalidToken() {
            given(jwtTokenProvider.validateToken("invalid-token"))
                    .willThrow(new RuntimeException("invalid"));

            assertThatThrownBy(() -> authService.reissue(new ReissueRequest("invalid-token")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(CommonErrorCode.INVALID_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("[실패] Redis에 저장된 토큰과 불일치하면 INVALID_REFRESH_TOKEN 예외를 던진다")
        void reissue_tokenMismatch() {
            given(jwtTokenProvider.validateToken("other-token")).willReturn(true);
            given(jwtTokenProvider.getMemberId("other-token")).willReturn(1L);
            given(tokenRedisService.getRefreshToken(1L)).willReturn("saved-token");

            assertThatThrownBy(() -> authService.reissue(new ReissueRequest("other-token")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(CommonErrorCode.INVALID_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("[실패] Redis에 저장된 토큰이 없으면 INVALID_REFRESH_TOKEN 예외를 던진다")
        void reissue_tokenNotInRedis() {
            given(jwtTokenProvider.validateToken("some-token")).willReturn(true);
            given(jwtTokenProvider.getMemberId("some-token")).willReturn(1L);
            given(tokenRedisService.getRefreshToken(1L)).willReturn(null);

            assertThatThrownBy(() -> authService.reissue(new ReissueRequest("some-token")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(CommonErrorCode.INVALID_REFRESH_TOKEN));
        }
    }

    // =============================================
    // logout
    // =============================================
    @Nested
    @DisplayName("로그아웃 — logout()")
    class LogoutTest {

        @Mock
        private HttpServletRequest httpRequest;

        @Test
        @DisplayName("[성공] 유효한 AccessToken으로 로그아웃 시 블랙리스트 등록 및 RefreshToken 삭제")
        void logout_validToken() {
            given(bearerTokenResolver.resolve(httpRequest)).willReturn("valid-access");
            given(jwtTokenProvider.getMemberId("valid-access")).willReturn(1L);
            given(jwtTokenProvider.getRemainingTime("valid-access")).willReturn(3600000L);

            authService.logout(httpRequest);

            then(tokenRedisService).should().addToBlacklist("valid-access", 3600000L);
            then(tokenRedisService).should().deleteRefreshToken(1L);
        }

        @Test
        @DisplayName("[성공] 만료된 AccessToken으로 로그아웃 시 RefreshToken만 삭제한다")
        void logout_expiredToken() {
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            given(claims.getSubject()).willReturn("1");
            given(bearerTokenResolver.resolve(httpRequest)).willReturn("expired-access");
            given(jwtTokenProvider.getMemberId("expired-access"))
                    .willThrow(new ExpiredJwtException(null, claims, "expired"));

            authService.logout(httpRequest);

            then(tokenRedisService).should(never()).addToBlacklist(anyString(), anyLong());
            then(tokenRedisService).should().deleteRefreshToken(1L);
        }

        @Test
        @DisplayName("[실패] 토큰이 없으면 UNAUTHORIZED 예외를 던진다")
        void logout_noToken() {
            given(bearerTokenResolver.resolve(httpRequest)).willReturn(null);

            assertThatThrownBy(() -> authService.logout(httpRequest))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(CommonErrorCode.UNAUTHORIZED));
        }
    }
}