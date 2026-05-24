package com.forpets.domain.sitter.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.MemberRole;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.member.repository.MemberRepository;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.entity.SitterSchedule;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.domain.sitter.repository.SitterScheduleRepository;
import com.forpets.global.security.jwt.JwtTokenProvider;
import com.forpets.global.security.jwt.TokenRedisService;
import io.jsonwebtoken.Jwts;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.oneOf;
import static org.hamcrest.Matchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SitterControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SitterProfileRepository sitterProfileRepository;

    @Autowired
    private SitterScheduleRepository sitterScheduleRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private TokenRedisService tokenRedisService;

    private Long authMemberId;
    private Long detailSitterWithScheduleId;
    private Long detailSitterWithoutScheduleId;
    private Long deletedSitterId;

    @BeforeEach
    void setUp() {
        given(tokenRedisService.isBlacklisted(anyString())).willReturn(false);

        saveSitter("integration-sitter-gwangjin@test.com", "integration-gwangjin",
                Region.GWANGJIN, PossiblePetType.DOG, PossiblePetSize.SMALL, 5, 20000);
        saveSitter("integration-sitter-gangnam@test.com", "integration-gangnam",
                Region.GANGNAM, PossiblePetType.CAT, PossiblePetSize.MEDIUM, 8, 30000);
        saveSitter("integration-sitter-all@test.com", "integration-all",
                Region.DONGJAK, PossiblePetType.ALL, PossiblePetSize.ALL, 10, 10000);

        SitterProfile detailSitterWithSchedule = saveSitter(
                "detail-sitter-with-schedule@test.com", "detail-with-schedule",
                Region.SEOCHO, PossiblePetType.DOG, PossiblePetSize.SMALL, 6, 25000);
        detailSitterWithScheduleId = detailSitterWithSchedule.getId();
        authMemberId = detailSitterWithSchedule.getMemberId();

        sitterScheduleRepository.save(SitterSchedule.builder()
                .sitterProfileId(detailSitterWithScheduleId)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .build());

        SitterProfile detailSitterWithoutSchedule = saveSitter(
                "detail-sitter-without-schedule@test.com", "detail-without-schedule",
                Region.GANGDONG, PossiblePetType.CAT, PossiblePetSize.MEDIUM, 2, 18000);
        detailSitterWithoutScheduleId = detailSitterWithoutSchedule.getId();

        SitterProfile deletedSitter = saveSitter(
                "detail-deleted-sitter@test.com", "detail-deleted",
                Region.MAPO, PossiblePetType.ALL, PossiblePetSize.ALL, 4, 22000);
        deletedSitterId = deletedSitter.getId();
        deletedSitter.delete();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("[성공] TC-01 파라미터 없이 전체 조회")
    void get_sitters_tc_01() throws Exception {
        mockMvc.perform(get("/api/sitters"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=3600"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").exists())
                .andExpect(jsonPath("$.data.totalPages").exists())
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.content[0]", hasKey("id")))
                .andExpect(jsonPath("$.data.content[0]", hasKey("memberId")))
                .andExpect(jsonPath("$.data.content[0]", hasKey("region")))
                .andExpect(jsonPath("$.data.content[0]", hasKey("experienceYears")))
                .andExpect(jsonPath("$.data.content[0]", hasKey("possiblePetType")))
                .andExpect(jsonPath("$.data.content[0]", hasKey("possiblePetSize")))
                .andExpect(jsonPath("$.data.content[0]", hasKey("pricePerHour")))
                .andExpect(jsonPath("$.data.content[0]", hasKey("status")))
                .andExpect(jsonPath("$.data.content[0]", hasKey("createdAt")))
                .andExpect(jsonPath("$.data.content[0]", hasKey("updatedAt")));
    }

    @Test
    @DisplayName("[성공] TC-02 region 단일 필터")
    void get_sitters_tc_02() throws Exception {
        mockMvc.perform(get("/api/sitters").param("region", "GWANGJIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[*].region", everyItem(is("GWANGJIN"))));
    }

    @Test
    @DisplayName("[성공] TC-03 possiblePetType 필터")
    void get_sitters_tc_03() throws Exception {
        mockMvc.perform(get("/api/sitters").param("possiblePetType", "DOG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[*].possiblePetType", everyItem(oneOf("DOG", "ALL"))));
    }

    @Test
    @DisplayName("[성공] TC-04 possiblePetSize 필터")
    void get_sitters_tc_04() throws Exception {
        mockMvc.perform(get("/api/sitters").param("possiblePetSize", "SMALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[*].possiblePetSize", everyItem(oneOf("SMALL", "ALL"))));
    }

    @Test
    @DisplayName("[성공] TC-05 가격 범위 필터")
    void get_sitters_tc_05() throws Exception {
        mockMvc.perform(get("/api/sitters")
                        .param("minPrice", "10000")
                        .param("maxPrice", "30000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[*].pricePerHour",
                        everyItem(allOf(greaterThanOrEqualTo(10000), lessThanOrEqualTo(30000)))));
    }

    @Test
    @DisplayName("[성공] TC-07 정렬 - pricePerHour")
    void get_sitters_tc_07() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/sitters").param("sort", "pricePerHour"))
                .andExpect(status().isOk())
                .andReturn();

        assertDescending(result, "pricePerHour");
    }

    @Test
    @DisplayName("[성공] TC-08 정렬 - experienceYears")
    void get_sitters_tc_08() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/sitters").param("sort", "experienceYears"))
                .andExpect(status().isOk())
                .andReturn();

        assertDescending(result, "experienceYears");
    }

    @Test
    @DisplayName("[실패] TC-12 허용되지 않은 sort 필드")
    void get_sitters_tc_12() throws Exception {
        mockMvc.perform(get("/api/sitters").param("sort", "hacked"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_SORT_FIELD"));
    }

    @Test
    @DisplayName("[실패] TC-13 page 음수")
    void get_sitters_tc_13() throws Exception {
        mockMvc.perform(get("/api/sitters").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PAGE_REQUEST"));
    }

    @Test
    @DisplayName("[실패] TC-15 size 최대값 초과")
    void get_sitters_tc_15() throws Exception {
        mockMvc.perform(get("/api/sitters").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PAGE_REQUEST"));
    }

    @Test
    @DisplayName("[실패] TC-16 잘못된 Enum 값 - region")
    void get_sitters_tc_16() throws Exception {
        mockMvc.perform(get("/api/sitters").param("region", "INVALID_REGION"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_SEARCH_CONDITION"));
    }

    @Test
    @DisplayName("[실패] TC-17 잘못된 Enum 값 - possiblePetType")
    void get_sitters_tc_17() throws Exception {
        mockMvc.perform(get("/api/sitters").param("possiblePetType", "BIRD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_SEARCH_CONDITION"));
    }

    @Test
    @DisplayName("[실패] TC-18 minPrice가 maxPrice보다 큰 경우")
    void get_sitters_tc_18() throws Exception {
        mockMvc.perform(get("/api/sitters")
                        .param("minPrice", "50000")
                        .param("maxPrice", "10000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_SEARCH_CONDITION"));
    }

    @Test
    @DisplayName("[성공] 상세 TC-01 로그인 상태에서 존재하는 sitterId로 조회")
    void get_sitter_detail_tc_01() throws Exception {
        mockMvc.perform(get("/api/sitters/{sitterId}", detailSitterWithScheduleId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=3600"))
                .andExpect(jsonPath("$.data", hasKey("id")))
                .andExpect(jsonPath("$.data", hasKey("memberId")))
                .andExpect(jsonPath("$.data", hasKey("region")))
                .andExpect(jsonPath("$.data", hasKey("introduction")))
                .andExpect(jsonPath("$.data", hasKey("experienceYears")))
                .andExpect(jsonPath("$.data", hasKey("possiblePetType")))
                .andExpect(jsonPath("$.data", hasKey("possiblePetSize")))
                .andExpect(jsonPath("$.data", hasKey("pricePerHour")))
                .andExpect(jsonPath("$.data", hasKey("status")))
                .andExpect(jsonPath("$.data", hasKey("schedules")))
                .andExpect(jsonPath("$.data", hasKey("createdAt")))
                .andExpect(jsonPath("$.data", hasKey("updatedAt")));
    }

    @Test
    @DisplayName("[성공] 상세 TC-02 schedules가 있는 시터 조회")
    void get_sitter_detail_tc_02() throws Exception {
        mockMvc.perform(get("/api/sitters/{sitterId}", detailSitterWithScheduleId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schedules.length()").value(1));
    }

    @Test
    @DisplayName("[성공] 상세 TC-03 schedules가 없는 시터 조회")
    void get_sitter_detail_tc_03() throws Exception {
        mockMvc.perform(get("/api/sitters/{sitterId}", detailSitterWithoutScheduleId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schedules").isArray())
                .andExpect(jsonPath("$.data.schedules.length()").value(0));
    }

    @Test
    @DisplayName("[성공] 상세 TC-04 응답 헤더 Cache-Control 확인")
    void get_sitter_detail_tc_04() throws Exception {
        mockMvc.perform(get("/api/sitters/{sitterId}", detailSitterWithScheduleId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=3600"));
    }

    @Test
    @DisplayName("[실패] 상세 TC-05 비로그인 상태에서 조회")
    void get_sitter_detail_tc_05() throws Exception {
        mockMvc.perform(get("/api/sitters/{sitterId}", detailSitterWithScheduleId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("[실패] 상세 TC-06 만료된 토큰으로 조회")
    void get_sitter_detail_tc_06() throws Exception {
        mockMvc.perform(get("/api/sitters/{sitterId}", detailSitterWithScheduleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("EXPIRED_TOKEN"));
    }

    @Test
    @DisplayName("[실패] 상세 TC-07 유효하지 않은 토큰으로 조회")
    void get_sitter_detail_tc_07() throws Exception {
        mockMvc.perform(get("/api/sitters/{sitterId}", detailSitterWithScheduleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid_token_value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("[실패] 상세 TC-08 존재하지 않는 sitterId 조회")
    void get_sitter_detail_tc_08() throws Exception {
        mockMvc.perform(get("/api/sitters/{sitterId}", 99999L)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SITTER_NOT_FOUND"));
    }

    @Test
    @DisplayName("[실패] 상세 TC-09 삭제된 시터 프로필 조회")
    void get_sitter_detail_tc_09() throws Exception {
        mockMvc.perform(get("/api/sitters/{sitterId}", deletedSitterId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SITTER_NOT_FOUND"));
    }

    @Test
    @DisplayName("[실패] 상세 TC-10 sitterId에 문자열 입력")
    void get_sitter_detail_tc_10() throws Exception {
        mockMvc.perform(get("/api/sitters/abc")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[실패] 상세 TC-11 sitterId에 음수 입력")
    void get_sitter_detail_tc_11() throws Exception {
        mockMvc.perform(get("/api/sitters/{sitterId}", -1L)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SITTER_NOT_FOUND"));
    }

    @Test
    @DisplayName("[성공] 상세 TC-12 createdAt / updatedAt 형식 확인")
    void get_sitter_detail_tc_12() throws Exception {
        String isoDateTimePattern = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*";

        mockMvc.perform(get("/api/sitters/{sitterId}", detailSitterWithScheduleId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdAt", matchesPattern(isoDateTimePattern)))
                .andExpect(jsonPath("$.data.updatedAt", matchesPattern(isoDateTimePattern)));
    }

    @Test
    @DisplayName("[성공] 상세 TC-13 status Enum 값 확인")
    void get_sitter_detail_tc_13() throws Exception {
        mockMvc.perform(get("/api/sitters/{sitterId}", detailSitterWithScheduleId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", anyOf(is("RESERVABLE"), is("NON_RESERVABLE"))));
    }

    @Test
    @DisplayName("[성공] 상세 TC-14 pricePerHour 값 확인")
    void get_sitter_detail_tc_14() throws Exception {
        mockMvc.perform(get("/api/sitters/{sitterId}", detailSitterWithScheduleId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pricePerHour", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("[성공] 상세 TC-15 id / memberId 타입 확인")
    void get_sitter_detail_tc_15() throws Exception {
        mockMvc.perform(get("/api/sitters/{sitterId}", detailSitterWithScheduleId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", any(Number.class)))
                .andExpect(jsonPath("$.data.memberId", any(Number.class)));
    }

    private SitterProfile saveSitter(
            String email,
            String nickname,
            Region region,
            PossiblePetType possiblePetType,
            PossiblePetSize possiblePetSize,
            int experienceYears,
            int pricePerHour
    ) {
        Member member = Member.builder()
                .email(email)
                .password("password123")
                .nickname(nickname)
                .phone("010-0000-0000")
                .gender(MemberGender.UNKNOWN)
                .region(region)
                .build();
        member.changeRoleToSitter();
        Member savedMember = memberRepository.save(member);

        SitterProfile sitterProfile = SitterProfile.builder()
                .memberId(savedMember.getId())
                .introduction("integration test sitter")
                .experienceYears(experienceYears)
                .possiblePetType(possiblePetType)
                .possiblePetSize(possiblePetSize)
                .pricePerHour(pricePerHour)
                .build();
        sitterProfile.approve(savedMember.getId());

        return sitterProfileRepository.save(sitterProfile);
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(authMemberId, MemberRole.MEMBER);
    }

    private String expiredToken() {
        SecretKey secretKey = (SecretKey) ReflectionTestUtils.getField(jwtTokenProvider, "secretKey");
        Date now = new Date();
        Date issuedAt = new Date(now.getTime() - 7200000);
        Date expiration = new Date(now.getTime() - 3600000);

        return Jwts.builder()
                .subject(String.valueOf(authMemberId))
                .issuer("ForPetsAuthServer")
                .issuedAt(issuedAt)
                .expiration(expiration)
                .id(UUID.randomUUID().toString())
                .claim("role", MemberRole.MEMBER.name())
                .signWith(secretKey)
                .compact();
    }

    private void assertDescending(MvcResult result, String fieldName) throws Exception {
        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("content");

        List<Integer> values = new ArrayList<>();
        content.forEach(item -> values.add(item.path(fieldName).asInt()));

        assertThat(values).isSortedAccordingTo((left, right) -> Integer.compare(right, left));
    }
}