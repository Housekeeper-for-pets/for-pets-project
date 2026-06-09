package com.forpets.domain.post.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.MemberRole;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.member.repository.MemberRepository;
import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.entity.PetGender;
import com.forpets.domain.pet.entity.PetSize;
import com.forpets.domain.pet.entity.PetSpecies;
import com.forpets.domain.pet.repository.PetRepository;
import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostPet;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.domain.post.entity.PostTimeSlot;
import com.forpets.domain.post.repository.PostPetRepository;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.post.repository.PostTimeSlotRepository;
import com.forpets.global.common.CareType;
import com.forpets.global.embed.entity.TimeSlotInfo;
import com.forpets.global.security.jwt.JwtTokenProvider;
import com.forpets.global.security.jwt.TokenRedisService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostPetRepository postPetRepository;

    @Autowired
    private PostTimeSlotRepository postTimeSlotRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private TokenRedisService tokenRedisService;

    private Long authMemberId;
    private Long noClosedMemberId;
    private Long detailPostId;
    private Long multiPetPostId;
    private Long multiTimeSlotPostId;
    private Long deletedPostId;

    @BeforeEach
    void setUp() {
        given(tokenRedisService.isBlacklisted(anyString())).willReturn(false);

        Member gangnamWriter = saveMember("post-gangnam@test.com", "post-gangnam", Region.GANGNAM);
        Member seochoWriter = saveMember("post-seocho@test.com", "post-seocho", Region.SEOCHO);
        Member authWriter = saveMember("post-auth@test.com", "post-auth", Region.GANGNAM);
        Member noClosedWriter = saveMember("post-no-closed@test.com", "post-no-closed", Region.DONGJAK);

        authMemberId = authWriter.getId();
        noClosedMemberId = noClosedWriter.getId();

        savePost(gangnamWriter, "codex-keyword 강아지 산책", "codex-keyword 강아지 방문 돌봄",
                CareType.VISIT, 90000, PostStatus.OPEN, 1, 1);
        savePost(gangnamWriter, "codex-dog 강아지 돌봄", "codex-dog 강아지 방문 케어",
                CareType.VISIT, 80000, PostStatus.OPEN, 1, 1);
        savePost(seochoWriter, "고양이 위탁", "고양이 위탁 케어",
                CareType.BOARDING, 30000, PostStatus.CLOSED, 1, 1);

        Post detailPost = savePost(gangnamWriter, "상세 공고", "상세 공고 내용",
                CareType.VISIT, 50000, PostStatus.OPEN, 1, 1);
        detailPostId = detailPost.getId();

        Post multiPetPost = savePost(gangnamWriter, "여러 반려동물 공고", "여러 반려동물 공고 내용",
                CareType.VISIT, 60000, PostStatus.OPEN, 2, 1);
        multiPetPostId = multiPetPost.getId();

        Post multiTimeSlotPost = savePost(gangnamWriter, "여러 시간 공고", "여러 시간 공고 내용",
                CareType.VISIT, 70000, PostStatus.OPEN, 1, 3);
        multiTimeSlotPostId = multiTimeSlotPost.getId();

        Post deletedPost = savePost(gangnamWriter, "삭제된 공고", "삭제된 공고 내용",
                CareType.VISIT, 10000, PostStatus.OPEN, 1, 1);
        deletedPostId = deletedPost.getId();
        deletedPost.delete();

        savePost(authWriter, "내 OPEN 공고", "내가 작성한 OPEN 공고",
                CareType.VISIT, 40000, PostStatus.OPEN, 1, 1);
        savePost(authWriter, "내 CLOSED 공고", "내가 작성한 CLOSED 공고",
                CareType.BOARDING, 20000, PostStatus.CLOSED, 1, 1);
        savePost(seochoWriter, "다른 회원 공고", "다른 회원 공고",
                CareType.VISIT, 100000, PostStatus.OPEN, 1, 1);
        savePost(noClosedWriter, "CLOSED 없는 회원 OPEN 공고", "CLOSED 없는 회원 OPEN 공고",
                CareType.VISIT, 15000, PostStatus.OPEN, 1, 1);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("공고 목록 조회 — GET /api/posts")
    class SearchPostsTest {

        @Test
        @DisplayName("[성공] TC-01 파라미터 없이 전체 조회")
        void search_posts_tc_01() throws Exception {
            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").exists())
                    .andExpect(jsonPath("$.data.totalPages").exists())
                    .andExpect(jsonPath("$.data.currentPage").value(0))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("id")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("memberId")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("title")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("content")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("region")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("careType")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("budgetAmount")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("status")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("pets")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("timeSlots")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("createdAt")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("updatedAt")));
        }

        @Test
        @DisplayName("[성공] TC-02 region 필터")
        void search_posts_tc_02() throws Exception {
            mockMvc.perform(get("/api/posts").param("region", "GANGNAM"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].region", everyItem(is("GANGNAM"))));
        }

        @Test
        @DisplayName("[성공] TC-03 careType 필터")
        void search_posts_tc_03() throws Exception {
            mockMvc.perform(get("/api/posts").param("careType", "VISIT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].careType", everyItem(is("VISIT"))));
        }

        @Test
        @DisplayName("[성공] TC-04 status 필터")
        void search_posts_tc_04() throws Exception {
            mockMvc.perform(get("/api/posts").param("status", "OPEN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].status", everyItem(is("OPEN"))));
        }

        @Test
        @DisplayName("[성공] TC-05 keyword 검색")
        void search_posts_tc_05() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/posts").param("keyword", "codex-keyword"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertKeywordIncluded(result, "codex-keyword");
        }

        @Test
        @DisplayName("[성공] TC-06 복합 조건 검색")
        void search_posts_tc_06() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/posts")
                            .param("region", "GANGNAM")
                            .param("careType", "VISIT")
                            .param("status", "OPEN")
                            .param("keyword", "codex-dog"))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode content = content(result);
            assertThat(content).isNotEmpty();
            content.forEach(item -> {
                assertThat(item.path("region").asText()).isEqualTo("GANGNAM");
                assertThat(item.path("careType").asText()).isEqualTo("VISIT");
                assertThat(item.path("status").asText()).isEqualTo("OPEN");
                assertThat(item.path("title").asText() + item.path("content").asText()).contains("codex-dog");
            });
        }

        @Test
        @DisplayName("[성공] TC-07 정렬 - updatedAt")
        void search_posts_tc_07() throws Exception {
            mockMvc.perform(get("/api/posts").param("sort", "updatedAt"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("[성공] TC-08 정렬 - budgetAmount")
        void search_posts_tc_08() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/posts").param("sort", "budgetAmount"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertDescending(result, "budgetAmount");
        }

        @Test
        @DisplayName("[성공] TC-09 페이징 - 2페이지 조회")
        void search_posts_tc_09() throws Exception {
            mockMvc.perform(get("/api/posts").param("page", "1").param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentPage").value(1))
                    .andExpect(jsonPath("$.data.size").value(5));
        }

        @Test
        @DisplayName("[성공] TC-10 size 최대값 경계 테스트")
        void search_posts_tc_10() throws Exception {
            mockMvc.perform(get("/api/posts").param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.size").value(50));
        }

        @Test
        @DisplayName("[성공] TC-11 결과 없는 조건")
        void search_posts_tc_11() throws Exception {
            mockMvc.perform(get("/api/posts").param("keyword", "절대없는키워드1234567890"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("[실패] TC-12 허용되지 않은 sort 필드")
        void search_posts_tc_12() throws Exception {
            mockMvc.perform(get("/api/posts").param("sort", "hacked"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_SORT_FIELD"));
        }

        @Test
        @DisplayName("[실패] TC-13 page 음수")
        void search_posts_tc_13() throws Exception {
            mockMvc.perform(get("/api/posts").param("page", "-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_PAGE_REQUEST"));
        }

        @Test
        @DisplayName("[실패] TC-14 size 0")
        void search_posts_tc_14() throws Exception {
            mockMvc.perform(get("/api/posts").param("size", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_PAGE_REQUEST"));
        }

        @Test
        @DisplayName("[실패] TC-15 size 최대값 초과")
        void search_posts_tc_15() throws Exception {
            mockMvc.perform(get("/api/posts").param("size", "51"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_PAGE_REQUEST"));
        }

        @Test
        @DisplayName("[실패] TC-16 잘못된 Enum 값 - careType")
        void search_posts_tc_16() throws Exception {
            mockMvc.perform(get("/api/posts").param("careType", "FLYING"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_CARE_TYPE"));
        }

        @Test
        @DisplayName("[실패] TC-17 잘못된 Enum 값 - status")
        void search_posts_tc_17() throws Exception {
            mockMvc.perform(get("/api/posts").param("status", "INVALID_STATUS"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_POST_STATUS"));
        }

        @Test
        @DisplayName("[실패] TC-18 잘못된 Enum 값 - region")
        void search_posts_tc_18() throws Exception {
            mockMvc.perform(get("/api/posts").param("region", "INVALID_REGION"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_SEARCH_CONDITION"));
        }
    }

    @Nested
    @DisplayName("공고 단건 조회 — GET /api/posts/{postId}")
    class GetPostTest {

        @Test
        @DisplayName("[성공] TC-01 존재하는 공고 단건 조회")
        void get_post_tc_01() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}", detailPostId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andExpect(jsonPath("$.data.id").value(detailPostId))
                    .andExpect(jsonPath("$.data.title").exists())
                    .andExpect(jsonPath("$.data.content").exists())
                    .andExpect(jsonPath("$.data.region").exists())
                    .andExpect(jsonPath("$.data.careType").exists())
                    .andExpect(jsonPath("$.data.budgetAmount").exists())
                    .andExpect(jsonPath("$.data.status").exists())
                    .andExpect(jsonPath("$.data.pets").isArray())
                    .andExpect(jsonPath("$.data.timeSlots").isArray())
                    .andExpect(jsonPath("$.data.createdAt").exists())
                    .andExpect(jsonPath("$.data.updatedAt").exists());
        }

        @Test
        @DisplayName("[성공] TC-02 인증 없이 조회 가능 확인")
        void get_post_tc_02() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}", detailPostId))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("[성공] TC-03 pets가 여러 마리인 공고 조회")
        void get_post_tc_03() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}", multiPetPostId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pets.length()").value(2));
        }

        @Test
        @DisplayName("[성공] TC-04 timeSlots가 여러 개인 공고 조회")
        void get_post_tc_04() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/posts/{postId}", multiTimeSlotPostId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.timeSlots.length()").value(3))
                    .andReturn();

            assertTimeSlotSequenceAscending(result);
        }

        @Test
        @DisplayName("[실패] TC-05 존재하지 않는 공고 조회")
        void get_post_tc_05() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}", 99999999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
        }

        @Test
        @DisplayName("[실패] TC-06 삭제된 공고 조회")
        void get_post_tc_06() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}", deletedPostId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
        }

        @Test
        @DisplayName("[성공] 단건 조회 응답 구조 검증")
        void get_post_tc_07() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}", detailPostId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pets[0].petId").exists())
                    .andExpect(jsonPath("$.data.pets[0].name").exists())
                    .andExpect(jsonPath("$.data.pets[0].species").exists())
                    .andExpect(jsonPath("$.data.pets[0].breed").exists())
                    .andExpect(jsonPath("$.data.pets[0].size").exists())
                    .andExpect(jsonPath("$.data.pets[0].age").exists())
                    .andExpect(jsonPath("$.data.pets[0].gender").exists())
                    .andExpect(jsonPath("$.data.timeSlots[0].timeSlotId").exists())
                    .andExpect(jsonPath("$.data.timeSlots[0].careDate").exists())
                    .andExpect(jsonPath("$.data.timeSlots[0].startTime").exists())
                    .andExpect(jsonPath("$.data.timeSlots[0].endTime").exists())
                    .andExpect(jsonPath("$.data.timeSlots[0].sequence").exists());
        }
    }

    @Nested
    @DisplayName("내가 작성한 공고 조회 — GET /api/posts/me")
    class GetMyPostsTest {

        @Test
        @DisplayName("[성공] TC-01 전체 내 공고 조회")
        void get_my_posts_tc_01() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/posts/me").header(HttpHeaders.AUTHORIZATION, bearerToken(authMemberId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").exists())
                    .andExpect(jsonPath("$.data.totalPages").exists())
                    .andExpect(jsonPath("$.data.currentPage").value(0))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andReturn();

            assertOnlyMemberPosts(result, authMemberId);
        }

        @Test
        @DisplayName("[성공] TC-02 status=OPEN 필터")
        void get_my_posts_tc_02() throws Exception {
            mockMvc.perform(get("/api/posts/me")
                            .param("status", "OPEN")
                            .header(HttpHeaders.AUTHORIZATION, bearerToken(authMemberId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].status", everyItem(is("OPEN"))));
        }

        @Test
        @DisplayName("[성공] TC-03 status=CLOSED 필터")
        void get_my_posts_tc_03() throws Exception {
            mockMvc.perform(get("/api/posts/me")
                            .param("status", "CLOSED")
                            .header(HttpHeaders.AUTHORIZATION, bearerToken(authMemberId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].status", everyItem(is("CLOSED"))));
        }

        @Test
        @DisplayName("[성공] TC-04 페이징")
        void get_my_posts_tc_04() throws Exception {
            mockMvc.perform(get("/api/posts/me")
                            .param("page", "0")
                            .param("size", "5")
                            .header(HttpHeaders.AUTHORIZATION, bearerToken(authMemberId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentPage").value(0))
                    .andExpect(jsonPath("$.data.size").value(5));
        }

        @Test
        @DisplayName("[성공] TC-05 결과 없는 경우")
        void get_my_posts_tc_05() throws Exception {
            mockMvc.perform(get("/api/posts/me")
                            .param("status", "CLOSED")
                            .header(HttpHeaders.AUTHORIZATION, bearerToken(noClosedMemberId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("[실패] TC-06 토큰 없이 요청")
        void get_my_posts_tc_06() throws Exception {
            mockMvc.perform(get("/api/posts/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("[실패] TC-07 잘못된 status 값")
        void get_my_posts_tc_07() throws Exception {
            mockMvc.perform(get("/api/posts/me")
                            .param("status", "INVALID")
                            .header(HttpHeaders.AUTHORIZATION, bearerToken(authMemberId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_POST_STATUS"));
        }

        @Test
        @DisplayName("[실패] TC-08 page 음수")
        void get_my_posts_tc_08() throws Exception {
            mockMvc.perform(get("/api/posts/me")
                            .param("page", "-1")
                            .header(HttpHeaders.AUTHORIZATION, bearerToken(authMemberId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_PAGE_REQUEST"));
        }

        @Test
        @DisplayName("[실패] TC-09 size 0")
        void get_my_posts_tc_09() throws Exception {
            mockMvc.perform(get("/api/posts/me")
                            .param("size", "0")
                            .header(HttpHeaders.AUTHORIZATION, bearerToken(authMemberId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_PAGE_REQUEST"));
        }

        @Test
        @DisplayName("[실패] TC-10 size 최대값 초과")
        void get_my_posts_tc_10() throws Exception {
            mockMvc.perform(get("/api/posts/me")
                            .param("size", "51")
                            .header(HttpHeaders.AUTHORIZATION, bearerToken(authMemberId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_PAGE_REQUEST"));
        }

        @Test
        @DisplayName("[성공] 내 공고 조회 응답 구조와 다른 회원 공고 제외 검증")
        void get_my_posts_tc_11() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/posts/me").header(HttpHeaders.AUTHORIZATION, bearerToken(authMemberId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0]", hasKey("id")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("memberId")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("title")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("content")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("region")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("careType")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("budgetAmount")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("status")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("pets")))
                    .andExpect(jsonPath("$.data.content[0]", hasKey("timeSlots")))
                    .andExpect(jsonPath("$.data.content[0].createdAt", notNullValue()))
                    .andExpect(jsonPath("$.data.content[0].updatedAt", notNullValue()))
                    .andReturn();

            assertOnlyMemberPosts(result, authMemberId);
        }
    }

    private Member saveMember(String email, String nickname, Region region) {
        return memberRepository.save(Member.builder()
                .email(email)
                .password("password123")
                .nickname(nickname)
                .phone("010-0000-0000")
                .gender(MemberGender.UNKNOWN)
                .region(region)
                .build());
    }

    private Post savePost(
            Member member,
            String title,
            String content,
            CareType careType,
            Integer budgetAmount,
            PostStatus status,
            int petCount,
            int timeSlotCount
    ) {
        Post post = postRepository.save(Post.builder()
                .memberId(member.getId())
                .title(title)
                .content(content)
                .careType(careType)
                .budgetAmount(budgetAmount)
                .build());

        if (status == PostStatus.CLOSED) {
            post.close();
        }

        List<Pet> pets = new ArrayList<>();
        for (int i = 1; i <= petCount; i++) {
            pets.add(petRepository.save(Pet.builder()
                    .memberId(member.getId())
                    .name("pet-" + title + "-" + i)
                    .species(i % 2 == 0 ? PetSpecies.CAT : PetSpecies.DOG)
                    .breed(i % 2 == 0 ? "코숏" : "푸들")
                    .size(i % 2 == 0 ? PetSize.MEDIUM : PetSize.SMALL)
                    .age(i + 1)
                    .gender(i % 2 == 0 ? PetGender.FEMALE : PetGender.MALE)
                    .build()));
        }

        postPetRepository.saveAll(pets.stream()
                .map(pet -> PostPet.createFrom(post.getId(), pet))
                .toList());

        List<PostTimeSlot> timeSlots = new ArrayList<>();
        for (int i = 1; i <= timeSlotCount; i++) {
            timeSlots.add(PostTimeSlot.create(
                    post.getId(),
                    TimeSlotInfo.of(
                            LocalDate.of(2026, 6, i),
                            LocalTime.of(9 + i, 0),
                            LocalTime.of(11 + i, 0),
                            i
                    )
            ));
        }
        postTimeSlotRepository.saveAll(timeSlots);

        return post;
    }

    private String bearerToken(Long memberId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(memberId, MemberRole.MEMBER);
    }

    private JsonNode content(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("content");
    }

    private void assertKeywordIncluded(MvcResult result, String keyword) throws Exception {
        JsonNode content = content(result);
        assertThat(content).isNotEmpty();
        content.forEach(item -> assertThat(item.path("title").asText() + item.path("content").asText())
                .contains(keyword));
    }

    private void assertDescending(MvcResult result, String fieldName) throws Exception {
        JsonNode content = content(result);
        List<Integer> values = new ArrayList<>();
        content.forEach(item -> {
            if (!item.path(fieldName).isNull()) {
                values.add(item.path(fieldName).asInt());
            }
        });

        assertThat(values).isSortedAccordingTo((left, right) -> Integer.compare(right, left));
    }

    private void assertTimeSlotSequenceAscending(MvcResult result) throws Exception {
        JsonNode timeSlots = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("timeSlots");
        List<Integer> sequences = new ArrayList<>();
        timeSlots.forEach(item -> sequences.add(item.path("sequence").asInt()));

        assertThat(sequences).isSorted();
    }

    private void assertOnlyMemberPosts(MvcResult result, Long memberId) throws Exception {
        JsonNode content = content(result);
        assertThat(content).isNotEmpty();
        content.forEach(item -> assertThat(item.path("memberId").asLong()).isEqualTo(memberId));
    }
}
