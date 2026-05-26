package com.forpets.domain.post.service;

import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.entity.PetGender;
import com.forpets.domain.pet.entity.PetSize;
import com.forpets.domain.pet.entity.PetSpecies;
import com.forpets.domain.pet.exception.PetErrorCode;
import com.forpets.domain.pet.exception.PetException;
import com.forpets.domain.pet.service.PetService;
import com.forpets.domain.post.dto.CreatePostRequest;
import com.forpets.domain.post.dto.PostResponseDto;
import com.forpets.domain.post.dto.UpdatePostRequest;
import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostPet;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.domain.post.entity.PostTimeSlot;
import com.forpets.domain.post.exception.PostErrorCode;
import com.forpets.domain.post.exception.PostException;
import com.forpets.domain.post.repository.PostPetRepository;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.post.repository.PostTimeSlotRepository;
import com.forpets.domain.proposal.entity.ProposalStatus;
import com.forpets.domain.proposal.exception.ProposalErrorCode;
import com.forpets.domain.proposal.exception.ProposalException;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.global.common.CareType;
import com.forpets.global.embed.TimeSlotValidator;
import com.forpets.global.embed.dto.TimeSlotRequest;
import com.forpets.global.embed.entity.PetSnapshot;
import com.forpets.global.embed.entity.TimeSlotInfo;
import com.forpets.global.embed.exception.TimeSlotErrorCode;
import com.forpets.global.embed.exception.TimeSlotException;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

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

    // ── 테스트 픽스처 ──
    private Member member1;       // 째길중 — 공고 작성자
    private Member member2;       // 타코맘 — 제3자
    private Pet pet1;             // member1의 타코
    private Post post;            // OPEN 상태 공고

    private final Long member1Id = 1L;
    private final Long member2Id = 2L;
    private final Long pet1Id = 10L;
    private final Long postId = 300L;

    @BeforeEach
    void setUp() {
        // member1: 째길중 (SEOCHO)
        member1 = Member.builder()
                .email("giljung@test.com")
                .password("password123")
                .nickname("째길중")
                .phone("010-1111-1111")
                .gender(MemberGender.MALE)
                .region(Region.SEOCHO)
                .build();
        ReflectionTestUtils.setField(member1, "id", member1Id);

        // member2: 타코맘 (DONGJAK)
        member2 = Member.builder()
                .email("jiwon@test.com")
                .password("password123")
                .nickname("타코맘")
                .phone("010-2222-2222")
                .gender(MemberGender.FEMALE)
                .region(Region.DONGJAK)
                .build();
        ReflectionTestUtils.setField(member2, "id", member2Id);

        // member1의 반려동물
        pet1 = Pet.builder()
                .memberId(member1Id)
                .name("타코")
                .species(PetSpecies.CAT)
                .breed("코리안숏헤어")
                .size(PetSize.SMALL)
                .age(4)
                .gender(PetGender.MALE)
                .note("길냥이에요, 많이 울어요, 귀여워요")
                .build();
        ReflectionTestUtils.setField(pet1, "id", pet1Id);

        // OPEN 상태 공고
        post = Post.builder()
                .memberId(member1Id)
                .title("타코 돌봐주실 분 구합니다")
                .content("3일간 출장이라 돌봐주실 분 찾아요")
                .careType(CareType.VISIT)
                .budgetAmount(30000)
                .build();
        ReflectionTestUtils.setField(post, "id", postId);
    }

    // ── 헬퍼 ──
    private List<TimeSlotRequest> validTimeSlots() {
        return List.of(
                new TimeSlotRequest(
                        LocalDate.now().plusDays(3),
                        LocalTime.of(10, 0),
                        LocalTime.of(14, 0)
                )
        );
    }

    private PostPet createPostPet(Long pId, Pet pet) {
        PostPet postPet = PostPet.createFrom(pId, pet);
        ReflectionTestUtils.setField(postPet, "id", pet.getId() + 100);
        return postPet;
    }

    private PostTimeSlot createPostTimeSlot(Long pId, int sequence) {
        TimeSlotInfo info = TimeSlotInfo.of(
                LocalDate.now().plusDays(3), LocalTime.of(10, 0), LocalTime.of(14, 0), sequence);
        PostTimeSlot slot = PostTimeSlot.create(pId, info);
        ReflectionTestUtils.setField(slot, "id", (long) (sequence + 400));
        return slot;
    }

    // ========================================================
    // 공고 등록 — POST /api/posts
    // ========================================================
    @Nested
    @DisplayName("공고 등록 — POST /api/posts")
    class CreatePostTest {

        @Test
        @DisplayName("[성공] 보호자가 공고 등록 성공 — 초기 상태 OPEN")
        void post_test_01() {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "타코 돌봐주실 분 구합니다", "3일간 출장이라 돌봐주실 분 찾아요",
                    List.of(pet1Id), CareType.VISIT, 30000, validTimeSlots()
            );
            given(memberService.findById(member1Id)).willReturn(member1);
            given(petService.findById(pet1Id)).willReturn(pet1);
            willDoNothing().given(timeSlotValidator).validate(anyList());
            given(postRepository.save(any(Post.class))).willReturn(post);
            given(postPetRepository.saveAll(anyList())).willReturn(List.of(createPostPet(postId, pet1)));
            given(postTimeSlotRepository.saveAll(anyList())).willReturn(List.of(createPostTimeSlot(postId, 1)));

            // when
            PostResponseDto result = postService.create(member1Id, request);

            // then
            assertThat(result.memberId()).isEqualTo(member1Id);
            assertThat(result.title()).isEqualTo("타코 돌봐주실 분 구합니다");
            assertThat(result.content()).isEqualTo("3일간 출장이라 돌봐주실 분 찾아요");
            assertThat(result.careType()).isEqualTo(CareType.VISIT);
            assertThat(result.budgetAmount()).isEqualTo(30000);
            assertThat(result.status()).isEqualTo(PostStatus.OPEN);
            assertThat(result.region()).isEqualTo(Region.SEOCHO);
            assertThat(result.pets()).hasSize(1);
            assertThat(result.timeSlots()).hasSize(1);
            then(postRepository).should().save(any(Post.class));
        }

        @Test
        @DisplayName("[실패] 타인의 반려동물로 공고 등록 시 차단")
        void post_test_02() {
            // given — member2의 pet으로 member1이 등록
            Pet otherPet = Pet.builder()
                    .memberId(member2Id)
                    .name("연두")
                    .species(PetSpecies.DOG)
                    .breed("포메라니안")
                    .size(PetSize.SMALL)
                    .age(1)
                    .gender(PetGender.FEMALE)
                    .build();
            ReflectionTestUtils.setField(otherPet, "id", 11L);

            CreatePostRequest request = new CreatePostRequest(
                    "제목", "내용", List.of(11L),
                    CareType.VISIT, 30000, validTimeSlots()
            );
            given(memberService.findById(member1Id)).willReturn(member1);
            given(petService.findById(11L)).willReturn(otherPet);

            // when & then
            assertThatThrownBy(() -> postService.create(member1Id, request))
                    .isInstanceOf(PetException.class)
                    .satisfies(ex -> assertThat(((PetException) ex).getErrorCode())
                            .isEqualTo(PetErrorCode.NOT_PET_OWNER));
        }

        @Test
        @DisplayName("[실패] 과거 날짜로 공고 등록 시 차단")
        void post_test_03() {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "제목", "내용", List.of(pet1Id),
                    CareType.VISIT, 30000, validTimeSlots()
            );
            given(memberService.findById(member1Id)).willReturn(member1);
            given(petService.findById(pet1Id)).willReturn(pet1);
            willThrow(new TimeSlotException(TimeSlotErrorCode.PAST_DATE_NOT_ALLOWED))
                    .given(timeSlotValidator).validate(anyList());

            // when & then
            assertThatThrownBy(() -> postService.create(member1Id, request))
                    .isInstanceOf(TimeSlotException.class)
                    .satisfies(ex -> assertThat(((TimeSlotException) ex).getErrorCode())
                            .isEqualTo(TimeSlotErrorCode.PAST_DATE_NOT_ALLOWED));
        }
    }

    // ========================================================
    // 공고 수정 — PUT /api/posts/{postId}
    // ========================================================
    @Nested
    @DisplayName("공고 수정 — PUT /api/posts/{postId}")
    class UpdatePostTest {

        @Test
        @DisplayName("[성공] OPEN 상태 공고 수정 성공 — PostPet, PostTimeSlot 전체 교체")
        void post_test_04() {
            // given
            UpdatePostRequest request = new UpdatePostRequest(
                    "수정된 제목", "수정된 내용",
                    List.of(pet1Id), CareType.BOARDING, 50000, validTimeSlots()
            );
            given(memberService.findById(member1Id)).willReturn(member1);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(proposalRepository.existsByPostIdAndStatusIn(eq(postId), anyList())).willReturn(false);
            given(petService.findById(pet1Id)).willReturn(pet1);
            willDoNothing().given(timeSlotValidator).validate(anyList());
            given(postPetRepository.saveAll(anyList())).willReturn(List.of(createPostPet(postId, pet1)));
            given(postTimeSlotRepository.saveAll(anyList())).willReturn(List.of(createPostTimeSlot(postId, 1)));

            // when
            PostResponseDto result = postService.update(member1Id, postId, request);

            // then
            assertThat(result.title()).isEqualTo("수정된 제목");
            assertThat(result.content()).isEqualTo("수정된 내용");
            assertThat(result.careType()).isEqualTo(CareType.BOARDING);
            assertThat(result.budgetAmount()).isEqualTo(50000);
            then(postPetRepository).should().deleteAllByPostId(postId);
            then(postPetRepository).should().flush();
            then(postTimeSlotRepository).should().deleteAllByPostId(postId);
            then(postTimeSlotRepository).should().flush();
        }

        @Test
        @DisplayName("[실패] 본인 공고가 아닌 공고 수정 시 차단")
        void post_test_05() {
            // given
            UpdatePostRequest request = new UpdatePostRequest(
                    "수정", "내용", List.of(pet1Id),
                    CareType.VISIT, 30000, validTimeSlots()
            );
            given(memberService.findById(member2Id)).willReturn(member2);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> postService.update(member2Id, postId, request))
                    .isInstanceOf(PostException.class)
                    .satisfies(ex -> assertThat(((PostException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.NOT_POST_AUTHOR));
        }

        @Test
        @DisplayName("[실패] CLOSED 상태 공고 수정 시 차단")
        void post_test_06() {
            // given
            post.close(); // CLOSED 상태로 변경
            UpdatePostRequest request = new UpdatePostRequest(
                    "수정", "내용", List.of(pet1Id),
                    CareType.VISIT, 30000, validTimeSlots()
            );
            given(memberService.findById(member1Id)).willReturn(member1);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> postService.update(member1Id, postId, request))
                    .isInstanceOf(PostException.class)
                    .satisfies(ex -> assertThat(((PostException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.POST_NOT_OPEN));
        }

        @Test
        @DisplayName("[실패] active Proposal이 있는 공고 수정 시 차단")
        void post_test_07() {
            // given
            UpdatePostRequest request = new UpdatePostRequest(
                    "수정", "내용", List.of(pet1Id),
                    CareType.VISIT, 30000, validTimeSlots()
            );
            given(memberService.findById(member1Id)).willReturn(member1);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(proposalRepository.existsByPostIdAndStatusIn(
                    eq(postId), eq(List.of(ProposalStatus.PENDING, ProposalStatus.ACCEPTED))))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> postService.update(member1Id, postId, request))
                    .isInstanceOf(ProposalException.class)
                    .satisfies(ex -> assertThat(((ProposalException) ex).getErrorCode())
                            .isEqualTo(ProposalErrorCode.HAS_ACTIVE_PROPOSAL));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 공고 수정 시도")
        void post_test_08() {
            // given
            UpdatePostRequest request = new UpdatePostRequest(
                    "수정", "내용", List.of(pet1Id),
                    CareType.VISIT, 30000, validTimeSlots()
            );
            given(memberService.findById(member1Id)).willReturn(member1);
            given(postRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.update(member1Id, 999L, request))
                    .isInstanceOf(PostException.class)
                    .satisfies(ex -> assertThat(((PostException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.POST_NOT_FOUND));
        }
    }

    // ========================================================
    // 공고 닫기 — PATCH /api/posts/{postId}/close
    // ========================================================
    @Nested
    @DisplayName("공고 닫기 — PATCH /api/posts/{postId}/close")
    class ClosePostTest {

        @Test
        @DisplayName("[성공] OPEN 상태 공고 CLOSED 변경 성공")
        void post_test_09() {
            // given
            given(memberService.findById(member1Id)).willReturn(member1);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(proposalRepository.existsByPostIdAndStatusIn(eq(postId), anyList())).willReturn(false);
            given(postPetRepository.findAllByPostId(postId)).willReturn(List.of(createPostPet(postId, pet1)));
            given(postTimeSlotRepository.findAllByPostIdOrderByTimeSlotInfoSequence(postId))
                    .willReturn(List.of(createPostTimeSlot(postId, 1)));

            // when
            PostResponseDto result = postService.closePost(member1Id, postId);

            // then
            assertThat(result.status()).isEqualTo(PostStatus.CLOSED);
        }

        @Test
        @DisplayName("[실패] 본인 공고가 아닌 공고 닫기 시 차단")
        void post_test_10() {
            // given
            given(memberService.findById(member2Id)).willReturn(member2);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> postService.closePost(member2Id, postId))
                    .isInstanceOf(PostException.class)
                    .satisfies(ex -> assertThat(((PostException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.NOT_POST_AUTHOR));
        }

        @Test
        @DisplayName("[실패] active Proposal이 있는 공고 닫기 시 차단")
        void post_test_11() {
            // given
            given(memberService.findById(member1Id)).willReturn(member1);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(proposalRepository.existsByPostIdAndStatusIn(
                    eq(postId), eq(List.of(ProposalStatus.PENDING, ProposalStatus.ACCEPTED))))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> postService.closePost(member1Id, postId))
                    .isInstanceOf(ProposalException.class)
                    .satisfies(ex -> assertThat(((ProposalException) ex).getErrorCode())
                            .isEqualTo(ProposalErrorCode.HAS_ACTIVE_PROPOSAL));
        }

        @Test
        @DisplayName("[실패] 이미 CLOSED 상태 공고 닫기 시 차단")
        void post_test_12() {
            // given
            post.close(); // 사전에 CLOSED 상태로 변경
            given(memberService.findById(member1Id)).willReturn(member1);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(proposalRepository.existsByPostIdAndStatusIn(eq(postId), anyList())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> postService.closePost(member1Id, postId))
                    .isInstanceOf(PostException.class)
                    .satisfies(ex -> assertThat(((PostException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.INVALID_STATUS_TRANSITION));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 공고 닫기 시도")
        void post_test_13() {
            // given
            given(memberService.findById(member1Id)).willReturn(member1);
            given(postRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.closePost(member1Id, 999L))
                    .isInstanceOf(PostException.class)
                    .satisfies(ex -> assertThat(((PostException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.POST_NOT_FOUND));
        }
    }

    // ========================================================
    // 공고 삭제 — DELETE /api/posts/{postId}
    // ========================================================
    @Nested
    @DisplayName("공고 삭제 — DELETE /api/posts/{postId}")
    class DeletePostTest {

        @Test
        @DisplayName("[성공] active Proposal 없는 공고 삭제 성공")
        void post_test_14() {
            // given
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(proposalRepository.existsByPostIdAndStatusIn(eq(postId), anyList())).willReturn(false);

            // when
            postService.delete(member1Id, postId);

            // then
            assertThat(post.isDeleted()).isTrue();
            assertThat(post.getDeletedAt()).isNotNull();
            assertThat(post.getStatus()).isEqualTo(PostStatus.CLOSED);
        }

        @Test
        @DisplayName("[실패] 본인 공고가 아닌 공고 삭제 시 차단")
        void post_test_15() {
            // given
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> postService.delete(member2Id, postId))
                    .isInstanceOf(PostException.class)
                    .satisfies(ex -> assertThat(((PostException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.NOT_POST_AUTHOR));
        }

        @Test
        @DisplayName("[실패] active Proposal이 있는 공고 삭제 시 차단")
        void post_test_16() {
            // given
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(proposalRepository.existsByPostIdAndStatusIn(
                    eq(postId), eq(List.of(ProposalStatus.PENDING, ProposalStatus.ACCEPTED))))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> postService.delete(member1Id, postId))
                    .isInstanceOf(ProposalException.class)
                    .satisfies(ex -> assertThat(((ProposalException) ex).getErrorCode())
                            .isEqualTo(ProposalErrorCode.HAS_ACTIVE_PROPOSAL));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 공고 삭제 시도")
        void post_test_17() {
            // given
            given(postRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.delete(member1Id, 999L))
                    .isInstanceOf(PostException.class)
                    .satisfies(ex -> assertThat(((PostException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.POST_NOT_FOUND));
        }
    }
}