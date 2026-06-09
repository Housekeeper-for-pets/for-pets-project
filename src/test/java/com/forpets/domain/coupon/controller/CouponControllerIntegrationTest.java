package com.forpets.domain.coupon.controller;

import com.forpets.domain.coupon.entity.Coupon;
import com.forpets.domain.coupon.entity.UserCoupon;
import com.forpets.domain.coupon.repository.CouponRepository;
import com.forpets.domain.coupon.repository.UserCouponRepository;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.MemberRole;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Coupon 통합 테스트")
class CouponControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private UserCouponRepository userCouponRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisService tokenRedisService;

    private Member member;
    private Member adminMember;
    private Coupon coupon;

    @BeforeEach
    void setUp() {
        given(tokenRedisService.isBlacklisted(anyString())).willReturn(false);

        member = memberRepository.save(Member.builder()
                .email("coupon-member@example.com")
                .password(passwordEncoder.encode("password123!"))
                .nickname("쿠폰테스터")
                .gender(MemberGender.MALE)
                .region(Region.GANGNAM)
                .build());

        adminMember = memberRepository.save(Member.builder()
                .email("coupon-admin@example.com")
                .password(passwordEncoder.encode("password123!"))
                .nickname("관리자쿠폰")
                .gender(MemberGender.UNKNOWN)
                .region(Region.UNKNOWN)
                .build());
        ReflectionTestUtils.setField(adminMember, "role", MemberRole.ADMIN);

        coupon = couponRepository.save(Coupon.builder()
                .name("10% 할인 쿠폰")
                .discountRate(10)
                .totalQuantity(100)
                .build());
    }

    // =============================================
    // 쿠폰 발급
    // =============================================
    @Nested
    @DisplayName("쿠폰 발급 — POST /api/coupons/{couponId}/issue")
    class IssueCouponTest {

        @Test
        @DisplayName("[성공] 정상 발급 시 201과 발급 정보를 반환한다")
        void issueCoupon_success() throws Exception {
            String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());

            mockMvc.perform(post("/api/coupons/{couponId}/issue", coupon.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.couponName").value("10% 할인 쿠폰"));
        }

        @Test
        @DisplayName("[실패] 미인증 요청이면 401을 반환한다")
        void issueCoupon_unauthorized() throws Exception {
            mockMvc.perform(post("/api/coupons/{couponId}/issue", coupon.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[실패] ADMIN은 쿠폰을 발급받을 수 없어 403을 반환한다")
        void issueCoupon_adminForbidden() throws Exception {
            String token = jwtTokenProvider.createAccessToken(adminMember.getId(), adminMember.getRole());

            mockMvc.perform(post("/api/coupons/{couponId}/issue", coupon.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[실패] 재고 소진 시 400과 COUPON_QUANTITY_EXHAUSTED를 반환한다")
        void issueCoupon_quantityExhausted() throws Exception {
            ReflectionTestUtils.setField(coupon, "remainingQuantity", 0);

            String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());

            mockMvc.perform(post("/api/coupons/{couponId}/issue", coupon.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("COUPON_QUANTITY_EXHAUSTED"));
        }

        @Test
        @DisplayName("[실패] 이미 발급받은 쿠폰이면 409와 COUPON_ALREADY_ISSUED를 반환한다")
        void issueCoupon_alreadyIssued() throws Exception {
            // 이미 발급된 이력 생성
            userCouponRepository.save(UserCoupon.builder()
                    .userId(member.getId())
                    .couponId(coupon.getId())
                    .build());

            String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());

            mockMvc.perform(post("/api/coupons/{couponId}/issue", coupon.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("COUPON_ALREADY_ISSUED"));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 쿠폰이면 404와 COUPON_NOT_FOUND를 반환한다")
        void issueCoupon_couponNotFound() throws Exception {
            String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());

            mockMvc.perform(post("/api/coupons/{couponId}/issue", 99999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("COUPON_NOT_FOUND"));
        }
    }
}