package com.forpets.domain.proposal.service;

import com.forpets.domain.notification.broker.NotificationMessageBroker;
import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostPet;
import com.forpets.domain.post.entity.PostTimeSlot;
import com.forpets.domain.post.exception.PostErrorCode;
import com.forpets.domain.post.exception.PostException;
import com.forpets.domain.post.service.PostService;
import com.forpets.domain.proposal.dto.CreateProposalRequest;
import com.forpets.domain.proposal.dto.ProposalResponseDto;
import com.forpets.domain.proposal.entity.Proposal;
import com.forpets.domain.proposal.entity.ProposalStatus;
import com.forpets.domain.proposal.exception.ProposalErrorCode;
import com.forpets.domain.proposal.exception.ProposalException;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.entity.PetGender;
import com.forpets.domain.pet.entity.PetSize;
import com.forpets.domain.pet.entity.PetSpecies;
import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import com.forpets.domain.reservation.service.ReservationService;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.service.SitterService;
import com.forpets.global.common.CareType;
import com.forpets.global.embed.entity.TimeSlotInfo;
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
class ProposalServiceTest {

    @InjectMocks
    private ProposalService proposalService;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private SitterService sitterService;

    @Mock
    private ReservationService reservationService;

    @Mock
    private PostService postService;

    @Mock
    private NotificationMessageBroker notificationBroker;

    // ── 테스트 픽스처 ──
    private Post post;                   // member1의 OPEN 공고
    private SitterProfile sitterProfile; // member2의 시터 프로필
    private Proposal proposal;           // PENDING 상태 제안
    private Pet pet1;                    // member1의 타코

    private final Long member1Id = 1L;   // 째길중 — 공고 작성자
    private final Long member2Id = 2L;   // 타코맘 — 시터
    private final Long member3Id = 3L;   // 지민냥 — 제3자
    private final Long postId = 300L;
    private final Long sitterProfileId = 100L;
    private final Long proposalId = 500L;
    private final Long pet1Id = 10L;

    @BeforeEach
    void setUp() {
        // member1의 OPEN 공고
        post = Post.builder()
                .memberId(member1Id)
                .title("타코 돌봐주실 분 구합니다")
                .content("3일간 출장이라 돌봐주실 분 찾아요")
                .careType(CareType.VISIT)
                .budgetAmount(30000)
                .build();
        ReflectionTestUtils.setField(post, "id", postId);

        // member2의 시터 프로필
        sitterProfile = SitterProfile.builder()
                .memberId(member2Id)
                .introduction("고양이 전문 시터")
                .experienceYears(3)
                .possiblePetType(PossiblePetType.CAT)
                .possiblePetSize(PossiblePetSize.ALL)
                .pricePerHour(15000)
                .build();
        ReflectionTestUtils.setField(sitterProfile, "id", sitterProfileId);
        sitterProfile.approve(member3Id);

        // PENDING 상태 제안
        proposal = Proposal.builder()
                .postId(postId)
                .sitterProfileId(sitterProfileId)
                .sitterMemberId(member2Id)
                .memberId(member1Id)
                .proposedPrice(25000)
                .message("잘 돌봐드리겠습니다")
                .build();
        ReflectionTestUtils.setField(proposal, "id", proposalId);

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
    }

    // ── 헬퍼 ──
    private PostTimeSlot createPostTimeSlot(Long pId, int sequence) {
        TimeSlotInfo info = TimeSlotInfo.of(
                LocalDate.now().plusDays(3), LocalTime.of(10, 0), LocalTime.of(14, 0), sequence);
        PostTimeSlot slot = PostTimeSlot.create(pId, info);
        ReflectionTestUtils.setField(slot, "id", (long) (sequence + 400));
        return slot;
    }

    private PostPet createPostPet(Long pId, Pet pet) {
        PostPet postPet = PostPet.createFrom(pId, pet);
        ReflectionTestUtils.setField(postPet, "id", pet.getId() + 100);
        return postPet;
    }

    // ========================================================
    // 제안 등록 — POST /api/proposals/posts/{postId}
    // ========================================================
    @Nested
    @DisplayName("제안 등록 — POST /api/proposals/posts/{postId}")
    class CreateProposalTest {

        @Test
        @DisplayName("[성공] 시터가 OPEN 공고에 제안 등록 성공")
        void proposal_test_01() {
            // given
            CreateProposalRequest request = new CreateProposalRequest(25000, "잘 돌봐드리겠습니다");
            List<PostTimeSlot> postTimeSlots = List.of(createPostTimeSlot(postId, 1));

            given(postService.findById(postId)).willReturn(post);
            given(sitterService.findApprovedByMemberId(member2Id)).willReturn(sitterProfile);
            given(proposalRepository.existsByPostIdAndSitterProfileIdAndStatus(
                    postId, sitterProfileId, ProposalStatus.PENDING)).willReturn(false);
            given(postService.findTimeSlotsByPostId(postId)).willReturn(postTimeSlots);
            given(reservationService.hasConfirmedConflict(eq(sitterProfileId), anyList())).willReturn(false);
            given(proposalRepository.save(any(Proposal.class))).willReturn(proposal);

            // when
            ProposalResponseDto result = proposalService.create(member2Id, postId, request);

            // then
            assertThat(result.postId()).isEqualTo(postId);
            assertThat(result.sitterProfileId()).isEqualTo(sitterProfileId);
            assertThat(result.memberId()).isEqualTo(member1Id);

            assertThat(result.sitterMemberId()).isEqualTo(member2Id);
            assertThat(result.proposedPrice()).isEqualTo(25000);
            assertThat(result.status()).isEqualTo(ProposalStatus.PENDING);
            then(proposalRepository).should().save(any(Proposal.class));
        }

        @Test
        @DisplayName("[실패] CLOSED 공고에 제안 시도")
        void proposal_test_02() {
            // given
            post.close(); // CLOSED 상태로 변경
            CreateProposalRequest request = new CreateProposalRequest(25000, "제안");
            given(postService.findById(postId)).willReturn(post);

            // when & then
            assertThatThrownBy(() -> proposalService.create(member2Id, postId, request))
                    .isInstanceOf(PostException.class)
                    .satisfies(ex -> assertThat(((PostException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.POST_NOT_OPEN));
        }

        @Test
        @DisplayName("[실패] 본인 공고에 제안 시도")
        void proposal_test_03() {
            // given
            CreateProposalRequest request = new CreateProposalRequest(25000, "제안");
            given(postService.findById(postId)).willReturn(post);

            // when & then
            assertThatThrownBy(() -> proposalService.create(member1Id, postId, request))
                    .isInstanceOf(ProposalException.class)
                    .satisfies(ex -> assertThat(((ProposalException) ex).getErrorCode())
                            .isEqualTo(ProposalErrorCode.CANNOT_PROPOSE_OWN_POST));
        }

        @Test
        @DisplayName("[실패] 동일 공고에 PENDING 중복 제안 시도")
        void proposal_test_04() {
            // given
            CreateProposalRequest request = new CreateProposalRequest(25000, "중복 제안");
            given(postService.findById(postId)).willReturn(post);
            given(sitterService.findApprovedByMemberId(member2Id)).willReturn(sitterProfile);
            given(proposalRepository.existsByPostIdAndSitterProfileIdAndStatus(
                    postId, sitterProfileId, ProposalStatus.PENDING)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> proposalService.create(member2Id, postId, request))
                    .isInstanceOf(ProposalException.class)
                    .satisfies(ex -> assertThat(((ProposalException) ex).getErrorCode())
                            .isEqualTo(ProposalErrorCode.DUPLICATE_PROPOSAL));
        }

        @Test
        @DisplayName("[성공] WITHDRAWN/REJECTED 후 동일 공고 재제안 가능")
        void proposal_test_05() {
            // given — 기존 PENDING 없음 (WITHDRAWN/REJECTED 상태만 존재)
            CreateProposalRequest request = new CreateProposalRequest(28000, "재제안합니다");
            List<PostTimeSlot> postTimeSlots = List.of(createPostTimeSlot(postId, 1));

            Proposal newProposal = Proposal.builder()
                    .postId(postId)
                    .sitterProfileId(sitterProfileId)
                    .memberId(member2Id)
                    .proposedPrice(28000)
                    .message("재제안합니다")
                    .build();
            ReflectionTestUtils.setField(newProposal, "id", 501L);

            given(postService.findById(postId)).willReturn(post);
            given(sitterService.findApprovedByMemberId(member2Id)).willReturn(sitterProfile);
            given(proposalRepository.existsByPostIdAndSitterProfileIdAndStatus(
                    postId, sitterProfileId, ProposalStatus.PENDING)).willReturn(false);
            given(postService.findTimeSlotsByPostId(postId)).willReturn(postTimeSlots);
            given(reservationService.hasConfirmedConflict(eq(sitterProfileId), anyList())).willReturn(false);
            given(proposalRepository.save(any(Proposal.class))).willReturn(newProposal);

            // when
            ProposalResponseDto result = proposalService.create(member2Id, postId, request);

            // then
            assertThat(result.proposedPrice()).isEqualTo(28000);
            then(proposalRepository).should().save(any(Proposal.class));
        }

        @Test
        @DisplayName("[실패] CONFIRMED 예약과 시간 충돌 시 제안 불가")
        void proposal_test_06() {
            // given
            CreateProposalRequest request = new CreateProposalRequest(25000, "제안");
            List<PostTimeSlot> postTimeSlots = List.of(createPostTimeSlot(postId, 1));

            given(postService.findById(postId)).willReturn(post);
            given(sitterService.findApprovedByMemberId(member2Id)).willReturn(sitterProfile);
            given(proposalRepository.existsByPostIdAndSitterProfileIdAndStatus(
                    postId, sitterProfileId, ProposalStatus.PENDING)).willReturn(false);
            given(postService.findTimeSlotsByPostId(postId)).willReturn(postTimeSlots);
            given(reservationService.hasConfirmedConflict(eq(sitterProfileId), anyList())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> proposalService.create(member2Id, postId, request))
                    .isInstanceOf(ReservationException.class)
                    .satisfies(ex -> assertThat(((ReservationException) ex).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_CONFLICT));
        }

        @Test
        @DisplayName("[실패] 시터 프로필이 없는 회원이 제안 시도")
        void proposal_test_07() {
            // given
            CreateProposalRequest request = new CreateProposalRequest(25000, "제안");
            given(postService.findById(postId)).willReturn(post);
            given(sitterService.findApprovedByMemberId(member3Id))
                    .willThrow(new SitterException(SitterErrorCode.SITTER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> proposalService.create(member3Id, postId, request))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.SITTER_NOT_FOUND));
        }
    }

    // ========================================================
    // 공고에 들어온 제안 목록 조회 — GET /api/proposals/posts/{postId}
    // ========================================================
    @Nested
    @DisplayName("공고에 들어온 제안 목록 조회 — GET /api/proposals/posts/{postId}")
    class GetByPostIdTest {

        @Test
        @DisplayName("[성공] 공고 작성자가 제안 목록 조회 성공")
        void proposal_test_08() {
            // given
            given(postService.findById(postId)).willReturn(post);
            given(proposalRepository.findAllByPostId(postId)).willReturn(List.of(proposal));

            // when
            List<ProposalResponseDto> result = proposalService.getByPostId(member1Id, postId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).postId()).isEqualTo(postId);
        }

        @Test
        @DisplayName("[실패] 공고 작성자가 아닌 회원이 제안 목록 조회 시 차단")
        void proposal_test_09() {
            // given
            given(postService.findById(postId)).willReturn(post);

            // when & then
            assertThatThrownBy(() -> proposalService.getByPostId(member2Id, postId))
                    .isInstanceOf(PostException.class)
                    .satisfies(ex -> assertThat(((PostException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.NOT_POST_AUTHOR));
        }
    }

    // ========================================================
    // 내가 보낸 제안 목록 조회 — GET /api/proposals/me
    // ========================================================
    @Nested
    @DisplayName("내가 보낸 제안 목록 조회 — GET /api/proposals/me")
    class GetMyProposalsTest {

        @Test
        @DisplayName("[성공] 시터 본인이 보낸 제안 목록 조회 성공")
        void proposal_test_10() {
            // given
            given(sitterService.findApprovedByMemberId(member2Id)).willReturn(sitterProfile);
            given(proposalRepository.findAllBySitterProfileId(sitterProfileId)).willReturn(List.of(proposal));

            // when
            List<ProposalResponseDto> result = proposalService.getMyProposals(member2Id);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).sitterProfileId()).isEqualTo(sitterProfileId);
        }
    }

    // ========================================================
    // 제안 상세 조회 — GET /api/proposals/{proposalId}
    // ========================================================
    @Nested
    @DisplayName("제안 상세 조회 — GET /api/proposals/{proposalId}")
    class GetByIdTest {

        @Test
        @DisplayName("[성공] 공고 작성자가 제안 상세 조회 성공")
        void proposal_test_11() {
            // given
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(postService.findById(postId)).willReturn(post);

            // when
            ProposalResponseDto result = proposalService.getById(member1Id, proposalId);

            // then
            assertThat(result.id()).isEqualTo(proposalId);
            assertThat(result.proposedPrice()).isEqualTo(25000);
        }

        @Test
        @DisplayName("[성공] 제안한 시터가 제안 상세 조회 성공")
        void proposal_test_12() {
            // given
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(postService.findById(postId)).willReturn(post);

            // when
            ProposalResponseDto result = proposalService.getById(member2Id, proposalId);

            // then
            assertThat(result.id()).isEqualTo(proposalId);
            assertThat(result.sitterMemberId()).isEqualTo(member2Id);
        }

        @Test
        @DisplayName("[실패] 당사자가 아닌 회원이 상세 조회 시 차단")
        void proposal_test_13() {
            // given
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(postService.findById(postId)).willReturn(post);

            // when & then
            assertThatThrownBy(() -> proposalService.getById(member3Id, proposalId))
                    .isInstanceOf(ProposalException.class)
                    .satisfies(ex -> assertThat(((ProposalException) ex).getErrorCode())
                            .isEqualTo(ProposalErrorCode.NOT_PROPOSAL_PARTY));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 제안 조회")
        void proposal_test_14() {
            // given
            given(proposalRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> proposalService.getById(member1Id, 999L))
                    .isInstanceOf(ProposalException.class)
                    .satisfies(ex -> assertThat(((ProposalException) ex).getErrorCode())
                            .isEqualTo(ProposalErrorCode.PROPOSAL_NOT_FOUND));
        }
    }

    // ========================================================
    // 제안 채택 — PATCH /api/proposals/{proposalId}/accept
    // ========================================================
    @Nested
    @DisplayName("제안 채택 — PATCH /api/proposals/{proposalId}/accept")
    class AcceptProposalTest {

        @Test
        @DisplayName("[성공] 공고 작성자가 제안 채택 성공 — status ACCEPTED + Reservation 자동 생성")
        void proposal_test_15() {
            // given
            List<PostPet> postPets = List.of(createPostPet(postId, pet1));
            List<PostTimeSlot> postTimeSlots = List.of(createPostTimeSlot(postId, 1));

            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(postService.findById(postId)).willReturn(post);
            given(sitterService.findApprovedById(sitterProfileId)).willReturn(sitterProfile);
            given(postService.findPetsByPostId(postId)).willReturn(postPets);
            given(postService.findTimeSlotsByPostId(postId)).willReturn(postTimeSlots);
            given(reservationService.hasConfirmedConflict(eq(sitterProfileId), anyList())).willReturn(false);

            // when
            ProposalResponseDto result = proposalService.accept(member1Id, proposalId);

            // then
            assertThat(result.status()).isEqualTo(ProposalStatus.ACCEPTED);
            then(reservationService).should().createFromProposal(
                    eq(proposal), eq(post), eq(member2Id), eq(postPets), eq(postTimeSlots));
        }

        @Test
        @DisplayName("[실패] 공고 작성자가 아닌 회원이 채택 시도")
        void proposal_test_16() {
            // given
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(postService.findById(postId)).willReturn(post);
            given(sitterService.findApprovedById(sitterProfileId)).willReturn(sitterProfile);

            // when & then
            assertThatThrownBy(() -> proposalService.accept(member2Id, proposalId))
                    .isInstanceOf(PostException.class)
                    .satisfies(ex -> assertThat(((PostException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.NOT_POST_AUTHOR));
        }

        @Test
        @DisplayName("[실패] PENDING이 아닌 제안 채택 시도")
        void proposal_test_17() {
            // given
            proposal.reject(); // REJECTED 상태로 변경
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));

            // when & then
            assertThatThrownBy(() -> proposalService.accept(member1Id, proposalId))
                    .isInstanceOf(ProposalException.class)
                    .satisfies(ex -> assertThat(((ProposalException) ex).getErrorCode())
                            .isEqualTo(ProposalErrorCode.NOT_PENDING_PROPOSAL));
        }

        @Test
        @DisplayName("[실패] CLOSED 공고의 제안 채택 시도")
        void proposal_test_18() {
            // given
            post.close(); // CLOSED 상태로 변경
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(postService.findById(postId)).willReturn(post);
            given(sitterService.findApprovedById(sitterProfileId)).willReturn(sitterProfile);

            // when & then
            assertThatThrownBy(() -> proposalService.accept(member1Id, proposalId))
                    .isInstanceOf(PostException.class)
                    .satisfies(ex -> assertThat(((PostException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.POST_NOT_OPEN));
        }

        @Test
        @DisplayName("[실패] CONFIRMED 예약과 시간 충돌 시 채택 불가")
        void proposal_test_19() {
            // given
            List<PostTimeSlot> postTimeSlots = List.of(createPostTimeSlot(postId, 1));

            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(postService.findById(postId)).willReturn(post);
            given(sitterService.findApprovedById(sitterProfileId)).willReturn(sitterProfile);
            given(postService.findPetsByPostId(postId)).willReturn(List.of(createPostPet(postId, pet1)));
            given(postService.findTimeSlotsByPostId(postId)).willReturn(postTimeSlots);
            given(reservationService.hasConfirmedConflict(eq(sitterProfileId), anyList())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> proposalService.accept(member1Id, proposalId))
                    .isInstanceOf(ReservationException.class)
                    .satisfies(ex -> assertThat(((ReservationException) ex).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_CONFLICT));
        }
    }

    // ========================================================
    // 제안 거절 — PATCH /api/proposals/{proposalId}/reject
    // ========================================================
    @Nested
    @DisplayName("제안 거절 — PATCH /api/proposals/{proposalId}/reject")
    class RejectProposalTest {

        @Test
        @DisplayName("[성공] 공고 작성자가 제안 거절 성공")
        void proposal_test_20() {
            // given
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(postService.findById(postId)).willReturn(post);

            // when
            ProposalResponseDto result = proposalService.reject(member1Id, proposalId);

            // then
            assertThat(result.status()).isEqualTo(ProposalStatus.REJECTED);
        }

        @Test
        @DisplayName("[실패] 공고 작성자가 아닌 회원이 거절 시도")
        void proposal_test_21() {
            // given
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(postService.findById(postId)).willReturn(post);

            // when & then
            assertThatThrownBy(() -> proposalService.reject(member2Id, proposalId))
                    .isInstanceOf(PostException.class)
                    .satisfies(ex -> assertThat(((PostException) ex).getErrorCode())
                            .isEqualTo(PostErrorCode.NOT_POST_AUTHOR));
        }

        @Test
        @DisplayName("[실패] PENDING이 아닌 제안 거절 시도")
        void proposal_test_22() {
            // given
            proposal.accept(); // ACCEPTED 상태로 변경
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));

            // when & then
            assertThatThrownBy(() -> proposalService.reject(member1Id, proposalId))
                    .isInstanceOf(ProposalException.class)
                    .satisfies(ex -> assertThat(((ProposalException) ex).getErrorCode())
                            .isEqualTo(ProposalErrorCode.NOT_PENDING_PROPOSAL));
        }
    }

    // ========================================================
    // 제안 철회 — PATCH /api/proposals/{proposalId}/withdraw
    // ========================================================
    @Nested
    @DisplayName("제안 철회 — PATCH /api/proposals/{proposalId}/withdraw")
    class WithdrawProposalTest {

        @Test
        @DisplayName("[성공] 시터 본인이 제안 철회 성공")
        void proposal_test_23() {
            // given
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(sitterService.findApprovedByMemberId(member2Id)).willReturn(sitterProfile);

            // when
            ProposalResponseDto result = proposalService.withdraw(member2Id, proposalId);

            // then
            assertThat(result.status()).isEqualTo(ProposalStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("[실패] 본인 제안이 아닌 제안 철회 시도")
        void proposal_test_24() {
            // given — member3의 다른 시터 프로필
            SitterProfile otherSitter = SitterProfile.builder()
                    .memberId(member3Id)
                    .introduction("다른 시터")
                    .experienceYears(1)
                    .possiblePetType(PossiblePetType.DOG)
                    .possiblePetSize(PossiblePetSize.SMALL)
                    .pricePerHour(10000)
                    .build();
            ReflectionTestUtils.setField(otherSitter, "id", 101L);

            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(sitterService.findApprovedByMemberId(member3Id)).willReturn(otherSitter);

            // when & then
            assertThatThrownBy(() -> proposalService.withdraw(member3Id, proposalId))
                    .isInstanceOf(ProposalException.class)
                    .satisfies(ex -> assertThat(((ProposalException) ex).getErrorCode())
                            .isEqualTo(ProposalErrorCode.NOT_PROPOSAL_OWNER));
        }

        @Test
        @DisplayName("[실패] PENDING이 아닌 제안 철회 시도")
        void proposal_test_25() {
            // given
            proposal.accept(); // ACCEPTED 상태로 변경
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));

            // when & then
            assertThatThrownBy(() -> proposalService.withdraw(member2Id, proposalId))
                    .isInstanceOf(ProposalException.class)
                    .satisfies(ex -> assertThat(((ProposalException) ex).getErrorCode())
                            .isEqualTo(ProposalErrorCode.NOT_PENDING_PROPOSAL));
        }
    }
}
