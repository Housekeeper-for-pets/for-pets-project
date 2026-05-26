package com.forpets.domain.post.service;

import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.entity.PetGender;
import com.forpets.domain.pet.entity.PetSize;
import com.forpets.domain.pet.entity.PetSpecies;
import com.forpets.domain.pet.service.PetService;
import com.forpets.domain.post.dto.PostPageResponse;
import com.forpets.domain.post.dto.PostResponseDto;
import com.forpets.domain.post.dto.PostSearchCondition;
import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostPet;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.domain.post.entity.PostTimeSlot;
import com.forpets.domain.post.exception.PostErrorCode;
import com.forpets.domain.post.exception.PostException;
import com.forpets.domain.post.repository.PostPetRepository;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.post.repository.PostTimeSlotRepository;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.global.common.CareType;
import com.forpets.global.embed.TimeSlotValidator;
import com.forpets.global.embed.entity.TimeSlotInfo;
import com.forpets.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
class PostSearchServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostPetRepository postPetRepository;

    @Mock
    private PostTimeSlotRepository postTimeSlotRepository;

    @Mock
    private PetService petService;

    @Mock
    private TimeSlotValidator timeSlotValidator;

    @Mock
    private MemberService memberService;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private PostCacheService postCacheService;

    private final Long memberId = 1L;
    private final Long postId = 100L;
    
    private Member member;
    private Post post;
    private Pet pet;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email("post-writer@test.com")
                .password("password123")
                .nickname("post-writer")
                .phone("010-1111-1111")
                .gender(MemberGender.UNKNOWN)
                .region(Region.GANGNAM)
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);

        post = Post.builder()
                .memberId(memberId)
                .title("강아지 산책 공고")
                .content("강아지 산책을 도와주세요")
                .careType(CareType.VISIT)
                .budgetAmount(50000)
                .build();
        ReflectionTestUtils.setField(post, "id", postId);

        pet = Pet.builder()
                .memberId(memberId)
                .name("초코")
                .species(PetSpecies.DOG)
                .breed("푸들")
                .size(PetSize.SMALL)
                .age(3)
                .gender(PetGender.MALE)
                .build();
        ReflectionTestUtils.setField(pet, "id", 10L);
    }

    @Nested
    @DisplayName("공고 목록 조회 — GET /api/posts")
    class SearchPostsTest {

        @Test
        @DisplayName("[성공] 기본 조회 요청은 repository에 위임된다")
        void search_posts_test_01() {
            PostSearchCondition condition = new PostSearchCondition(null, null, null, null);
            PostPageResponse response = PostPageResponse.of(List.of(), 0, 0, 0, 10);
            given(postCacheService.searchPostings(eq(condition), eq(0), eq(10), eq("createdAt"))).willReturn(response);

            PostPageResponse result = postService.searchPosts(condition, 0, 10, "createdAt");

            assertThat(result).isEqualTo(response);
            then(postCacheService).should().searchPostings(eq(condition), eq(0), eq(10), eq("createdAt"));
        }

        @Test
        @DisplayName("[성공] 허용된 정렬 필드 updatedAt, budgetAmount는 조회 가능하다")
        void search_posts_test_02() {
            PostSearchCondition condition = new PostSearchCondition(null, null, null, null);
            given(postCacheService.searchPostings(eq(condition), eq(0), eq(10), anyString()))
                    .willReturn(PostPageResponse.of(List.of(), 0, 0, 0, 10));

            postService.searchPosts(condition, 0, 10, "updatedAt");
            postService.searchPosts(condition, 0, 10, "budgetAmount");

            then(postCacheService).should(times(2)).searchPostings(eq(condition), eq(0), eq(10), anyString());
        }

        @Test
        @DisplayName("[실패] 허용되지 않은 sort 필드는 INVALID_SORT_FIELD를 반환한다")
        void search_posts_test_03() {
            PostSearchCondition condition = new PostSearchCondition(null, null, null, null);

            assertThatThrownBy(() -> postService.searchPosts(condition, 0, 10, "hacked"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.INVALID_SORT_FIELD));
        }

        @Test
        @DisplayName("[실패] page가 음수이면 INVALID_PAGE_REQUEST를 반환한다")
        void search_posts_test_04() {
            PostSearchCondition condition = new PostSearchCondition(null, null, null, null);

            assertThatThrownBy(() -> postService.searchPosts(condition, -1, 10, "createdAt"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.INVALID_PAGE_REQUEST));
        }

        @Test
        @DisplayName("[실패] size가 0이면 INVALID_PAGE_REQUEST를 반환한다")
        void search_posts_test_05() {
            PostSearchCondition condition = new PostSearchCondition(null, null, null, null);

            assertThatThrownBy(() -> postService.searchPosts(condition, 0, 0, "createdAt"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.INVALID_PAGE_REQUEST));
        }

        @Test
        @DisplayName("[실패] size가 51이면 INVALID_PAGE_REQUEST를 반환한다")
        void search_posts_test_06() {
            PostSearchCondition condition = new PostSearchCondition(null, null, null, null);

            assertThatThrownBy(() -> postService.searchPosts(condition, 0, 51, "createdAt"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.INVALID_PAGE_REQUEST));
        }
    }

    @Nested
    @DisplayName("공고 단건 조회 — GET /api/posts/{postId}")
    class GetPostTest {

        @Test
        @DisplayName("[성공] 존재하는 공고 단건 조회 시 pets와 timeSlots를 포함한다")
        void get_post_test_01() {
            PostPet postPet = PostPet.createFrom(postId, pet);
            PostTimeSlot timeSlot = PostTimeSlot.create(
                    postId,
                    TimeSlotInfo.of(LocalDate.of(2026, 6, 1), LocalTime.of(10, 0), LocalTime.of(12, 0), 1)
            );
            ReflectionTestUtils.setField(timeSlot, "id", 20L);

            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(memberService.findById(memberId)).willReturn(member);
            given(postPetRepository.findAllByPostId(postId)).willReturn(List.of(postPet));
            given(postTimeSlotRepository.findAllByPostIdOrderByTimeSlotInfoSequence(postId)).willReturn(List.of(timeSlot));

            PostResponseDto result = postService.getPost(postId);

            assertThat(result.id()).isEqualTo(postId);
            assertThat(result.memberId()).isEqualTo(memberId);
            assertThat(result.region()).isEqualTo(Region.GANGNAM);
            assertThat(result.pets()).hasSize(1);
            assertThat(result.timeSlots()).hasSize(1);
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 공고는 POST_NOT_FOUND를 반환한다")
        void get_post_test_02() {
            given(postRepository.findById(99999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> postService.getPost(99999L))
                    .isInstanceOf(PostException.class)
                    .satisfies(ex -> assertThat(((PostException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.POST_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("내가 작성한 공고 조회 — GET /api/posts/me")
    class SearchMyPostsTest {

        @Test
        @DisplayName("[성공] 기본 조회 요청은 memberId 기준으로 repository에 위임된다")
        void search_my_posts_test_01() {
            PostPageResponse response = PostPageResponse.of(List.of(), 0, 0, 0, 10);
            given(postRepository.searchMyPosts(eq(memberId), eq(null), any())).willReturn(response);

            PostPageResponse result = postService.searchMyPosts(memberId, null, 0, 10);

            assertThat(result).isEqualTo(response);
            then(postRepository).should().searchMyPosts(eq(memberId), eq(null), any());
        }

        @Test
        @DisplayName("[성공] status=OPEN 필터 조회")
        void search_my_posts_test_02() {
            given(postRepository.searchMyPosts(eq(memberId), eq(PostStatus.OPEN), any()))
                    .willReturn(PostPageResponse.of(List.of(), 0, 0, 0, 10));

            postService.searchMyPosts(memberId, "OPEN", 0, 10);

            then(postRepository).should().searchMyPosts(eq(memberId), eq(PostStatus.OPEN), any());
        }

        @Test
        @DisplayName("[성공] status=CLOSED 필터 조회")
        void search_my_posts_test_03() {
            given(postRepository.searchMyPosts(eq(memberId), eq(PostStatus.CLOSED), any()))
                    .willReturn(PostPageResponse.of(List.of(), 0, 0, 0, 10));

            postService.searchMyPosts(memberId, "CLOSED", 0, 10);

            then(postRepository).should().searchMyPosts(eq(memberId), eq(PostStatus.CLOSED), any());
        }

        @Test
        @DisplayName("[실패] 잘못된 status 값은 INVALID_POST_STATUS를 반환한다")
        void search_my_posts_test_04() {
            assertThatThrownBy(() -> postService.searchMyPosts(memberId, "INVALID", 0, 10))
                    .isInstanceOf(PostException.class)
                    .satisfies(ex -> assertThat(((PostException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.INVALID_POST_STATUS));
        }

        @Test
        @DisplayName("[실패] page가 음수이면 INVALID_PAGE_REQUEST를 반환한다")
        void search_my_posts_test_05() {
            assertThatThrownBy(() -> postService.searchMyPosts(memberId, null, -1, 10))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.INVALID_PAGE_REQUEST));
        }

        @Test
        @DisplayName("[실패] size가 0이면 INVALID_PAGE_REQUEST를 반환한다")
        void search_my_posts_test_06() {
            assertThatThrownBy(() -> postService.searchMyPosts(memberId, null, 0, 0))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.INVALID_PAGE_REQUEST));
        }

        @Test
        @DisplayName("[실패] size가 51이면 INVALID_PAGE_REQUEST를 반환한다")
        void search_my_posts_test_07() {
            assertThatThrownBy(() -> postService.searchMyPosts(memberId, null, 0, 51))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.INVALID_PAGE_REQUEST));
        }
    }
}
