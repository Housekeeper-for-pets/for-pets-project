package com.forpets.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.auth.dto.LoginRequest;
import com.forpets.domain.auth.dto.ReissueRequest;
import com.forpets.domain.auth.dto.SignUpRequest;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.MemberStatus;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.member.repository.MemberRepository;
import com.forpets.global.security.jwt.JwtTokenProvider;
import com.forpets.global.security.jwt.TokenRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Auth 통합 테스트")
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @Autowired private EntityManager entityManager;

    @MockitoBean
    private TokenRedisService tokenRedisService;

    private Member savedMember;
    private static final String RAW_PASSWORD = "password123!";

    @BeforeEach
    void setUp() {
        given(tokenRedisService.isBlacklisted(anyString())).willReturn(false);

        savedMember = memberRepository.save(Member.builder()
                .email("auth-test@example.com")
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .nickname("인증테스터")
                .phone("010-1234-5678")
                .gender(MemberGender.MALE)
                .region(Region.GANGNAM)
                .build());
    }

    // =============================================
    // 회원가입
    // =============================================
    @Nested
    @DisplayName("회원가입 — POST /api/auth/signup")
    class SignUpTest {

        @Test
        @DisplayName("[성공] 정상 회원가입 시 201과 회원 정보를 반환한다")
        void signup_success() throws Exception {
            SignUpRequest request = new SignUpRequest(
                    "new@example.com", RAW_PASSWORD, "새회원",
                    "010-0000-0000", MemberGender.FEMALE, Region.SEOCHO
            );

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value("new@example.com"))
                    .andExpect(jsonPath("$.data.nickname").value("새회원"));
        }

        @Test
        @DisplayName("[실패] 이미 가입된 이메일이면 409와 EMAIL_DUPLICATED를 반환한다")
        void signup_emailDuplicated() throws Exception {
            SignUpRequest request = new SignUpRequest(
                    "auth-test@example.com", RAW_PASSWORD, "다른닉네임",
                    null, MemberGender.UNKNOWN, Region.UNKNOWN
            );

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("EMAIL_DUPLICATED"));
        }

        @Test
        @DisplayName("[실패] 이미 사용 중인 닉네임이면 409와 NICKNAME_DUPLICATED를 반환한다")
        void signup_nicknameDuplicated() throws Exception {
            SignUpRequest request = new SignUpRequest(
                    "other@example.com", RAW_PASSWORD, "인증테스터",
                    null, MemberGender.UNKNOWN, Region.UNKNOWN
            );

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("NICKNAME_DUPLICATED"));
        }
    }

    // =============================================
    // 로그인
    // =============================================
    @Nested
    @DisplayName("로그인 — POST /api/auth/login")
    class LoginTest {

        @Test
        @DisplayName("[성공] 정상 로그인 시 200과 토큰을 반환한다")
        void login_success() throws Exception {
            LoginRequest request = new LoginRequest("auth-test@example.com", RAW_PASSWORD);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("[실패] 비밀번호 불일치 시 401과 AUTHENTICATION_FAILED를 반환한다")
        void login_passwordMismatch() throws Exception {
            LoginRequest request = new LoginRequest("auth-test@example.com", "wrongPassword!");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));
        }

        @Test
        @DisplayName("[실패] 탈퇴한 계정으로 로그인 시 401과 AUTHENTICATION_FAILED를 반환한다 (@SQLRestriction으로 조회 불가)")
        void login_deletedAccount() throws Exception {
            savedMember.delete();
            entityManager.flush();

            LoginRequest request = new LoginRequest("auth-test@example.com", RAW_PASSWORD);

            // @SQLRestriction("deleted = false")로 인해 탈퇴 회원은 findByEmail() 조회 불가
            // → AUTHENTICATION_FAILED(401) 반환 — 보안상 계정 존재 여부를 노출하지 않는 설계
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));
        }

        @Test
        @DisplayName("[실패] 정지된 계정으로 로그인 시 403과 ACCOUNT_SUSPENDED를 반환한다")
        void login_suspendedAccount() throws Exception {
            ReflectionTestUtils.setField(savedMember, "status", MemberStatus.SUSPENDED);
            entityManager.merge(savedMember); // ReflectionTestUtils 변경분 DB 반영
            entityManager.flush();

            LoginRequest request = new LoginRequest("auth-test@example.com", RAW_PASSWORD);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("ACCOUNT_SUSPENDED"));
        }
    }

    // =============================================
    // 토큰 재발급
    // =============================================
    @Nested
    @DisplayName("토큰 재발급 — POST /api/auth/reissue")
    class ReissueTest {

        @Test
        @DisplayName("[성공] 유효한 RefreshToken으로 새 토큰 쌍을 반환한다")
        void reissue_success() throws Exception {
            String refreshToken = jwtTokenProvider.createRefreshToken(savedMember.getId());
            given(tokenRedisService.getRefreshToken(savedMember.getId())).willReturn(refreshToken);

            ReissueRequest request = new ReissueRequest(refreshToken);

            mockMvc.perform(post("/api/auth/reissue")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("[실패] 유효하지 않은 RefreshToken이면 401을 반환한다")
        void reissue_invalidToken() throws Exception {
            ReissueRequest request = new ReissueRequest("invalid.refresh.token");

            mockMvc.perform(post("/api/auth/reissue")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
        }
    }

    // =============================================
    // 로그아웃
    // =============================================
    @Nested
    @DisplayName("로그아웃 — POST /api/auth/logout")
    class LogoutTest {

        @Test
        @DisplayName("[성공] 유효한 AccessToken으로 로그아웃 시 200을 반환한다")
        void logout_success() throws Exception {
            String accessToken = jwtTokenProvider.createAccessToken(
                    savedMember.getId(), savedMember.getRole());

            mockMvc.perform(post("/api/auth/logout")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.message").value("로그아웃되었습니다."));
        }

        @Test
        @DisplayName("[실패] 토큰 없이 로그아웃 시 401을 반환한다")
        void logout_noToken() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isUnauthorized());
        }
    }
}