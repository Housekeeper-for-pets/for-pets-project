package com.forpets.domain.sitter.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.member.repository.MemberRepository;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.oneOf;
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

    @BeforeEach
    void setUp() {
        saveSitter("integration-sitter-gwangjin@test.com", "integration-gwangjin",
                Region.GWANGJIN, PossiblePetType.DOG, PossiblePetSize.SMALL, 5, 20000);
        saveSitter("integration-sitter-gangnam@test.com", "integration-gangnam",
                Region.GANGNAM, PossiblePetType.CAT, PossiblePetSize.MEDIUM, 8, 30000);
        saveSitter("integration-sitter-all@test.com", "integration-all",
                Region.DONGJAK, PossiblePetType.ALL, PossiblePetSize.ALL, 10, 10000);
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

    private void saveSitter(
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

        sitterProfileRepository.save(SitterProfile.builder()
                .memberId(savedMember.getId())
                .introduction("integration test sitter")
                .experienceYears(experienceYears)
                .possiblePetType(possiblePetType)
                .possiblePetSize(possiblePetSize)
                .pricePerHour(pricePerHour)
                .build());
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
