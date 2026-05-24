package com.forpets.domain.proposal.service;

import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostPet;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.domain.post.entity.PostTimeSlot;
import com.forpets.domain.post.exception.PostErrorCode;
import com.forpets.domain.post.exception.PostException;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.post.service.PostService;
import com.forpets.domain.proposal.dto.CreateProposalRequest;
import com.forpets.domain.proposal.dto.ProposalResponseDto;
import com.forpets.domain.proposal.entity.Proposal;
import com.forpets.domain.proposal.entity.ProposalStatus;
import com.forpets.domain.proposal.exception.ProposalErrorCode;
import com.forpets.domain.proposal.exception.ProposalException;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import com.forpets.domain.reservation.service.ReservationService;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.service.SitterService;
import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final SitterService sitterService;
    private final ReservationService reservationService;
    private final PostService postService;


    // ===== API =====

    /*
    제안 등록
    1. 공고가 OPEN 상태인지 확인
    2. 본인 공고가 아닌지 확인
    3. 동일 공고에 중복 제안 방지
    4. CONFIRMED 예약과 시간 충돌 검증
    5. 제안 생성 (초기 상태: PENDING)
     */
    @Transactional
    public ProposalResponseDto create(Long memberId, Long postId, CreateProposalRequest request) {
        Post post = postService.findById(postId);
        validatePostOpen(post);
        validateNotOwnPost(memberId, post);

        SitterProfile sitter = sitterService.findByMemberId(memberId);
        validateNoDuplicate(postId, sitter.getId());
        validateApproved(sitter);
        // 정책 수정으로 CONFIRMED 예약이 있어도 Proposal 은 언제든 받을 수 있음

        List<PostTimeSlot> postTimeSlots = postService.findTimeSlotsByPostId(postId);
        if (reservationService.hasConfirmedConflict(sitter.getId(), postTimeSlots)) {
            throw new ReservationException(ReservationErrorCode.RESERVATION_CONFLICT);
        }

        Proposal proposal = proposalRepository.save(Proposal.builder()
                .postId(postId)
                .sitterProfileId(sitter.getId())
                .sitterMemberId(sitter.getMemberId())
                .memberId(memberId)
                .proposedPrice(request.proposedPrice())
                .message(request.message())
                .build());

        /*
         V2: 등록 후 Event send 하는 로직 추가
         Kafka 로 Post 의 proposal 개수 저장 (모니터링)
         sitter 에게 메시지 : 제안이 등록되었습니다.
         공고 작성자에게 메시지 : 새로운 제안이 들어왔습니다.
         ... 또 뭐가 있을까
         */

        return ProposalResponseDto.from(proposal);
    }

    /*
    공고에 들어온 제안 목록 조회
    공고 작성자만 조회 가능
     */
    public List<ProposalResponseDto> getByPostId(Long memberId, Long postId) {
//        Post post = findPost(postId);
        Post post = postService.findById(postId);

        validatePostAuthor(memberId, post);

        return proposalRepository.findAllByPostId(postId).stream()
                .map(ProposalResponseDto::from)
                .toList();
    }

    /*
    내가 보낸 제안 목록 조회
    시터 본인의 제안만 조회
     */
    public List<ProposalResponseDto> getMyProposals(Long memberId) {
        SitterProfile sitter = sitterService.findByMemberId(memberId);

        return proposalRepository.findAllBySitterProfileId(sitter.getId()).stream()
                .map(ProposalResponseDto::from)
                .toList();
    }

    /*
    제안 상세 조회
    공고 작성자 또는 제안한 시터만 조회 가능
     */
    public ProposalResponseDto getById(Long memberId, Long proposalId) {
        Proposal proposal = findById(proposalId);
        validateParty(memberId, proposal);

        return ProposalResponseDto.from(proposal);
    }

    /*
    제안 채택
    1. PENDING 상태 검증
    2. 공고 작성자 검증
    3. 공고 OPEN 상태 검증
    4. 선택한 제안 → ACCEPTED
    5. 같은 공고의 나머지 PENDING 제안 → REJECTED
    6. 공고 상태 → CLOSED

    7. Reservation 자동 생성 (PENDING)
       - PostPet → ReservationPet 스냅샷 복사
       - PostTimeSlot → ReservationTimeSlot 복사
     */
    @Transactional
    public ProposalResponseDto accept(Long memberId, Long proposalId) {
        Proposal proposal = findById(proposalId);
        validatePending(proposal);

//        Post post = findPost(proposal.getPostId());
        Post post = postService.findById(proposal.getPostId());

        SitterProfile sitterProfile = sitterService.findById(proposal.getSitterProfileId());
        validatePostAuthor(memberId, post);
        validatePostOpen(post);

        List<PostPet> postPets = postService.findPetsByPostId(post.getId());
        List<PostTimeSlot> postTimeSlots = postService.findTimeSlotsByPostId(post.getId());

        if (reservationService.hasConfirmedConflict(sitterProfile.getId(), postTimeSlots)) {
            throw new ReservationException(ReservationErrorCode.RESERVATION_CONFLICT);
        }

        proposal.accept();

        reservationService.createFromProposal(proposal, post, sitterProfile.getMemberId(), postPets, postTimeSlots);

        // 비동기 처리 (eventListener 또는 kafka) 는 알림이나 로그.. 같은거 작성 할 때 쓰자 ~

        return ProposalResponseDto.from(proposal);
    }

    /*
    제안 거절
    - PENDING 상태만 거절 가능
    - 공고 작성자만 거절 가능
     */
    @Transactional
    public ProposalResponseDto reject(Long memberId, Long proposalId) {
        Proposal proposal = findById(proposalId);
        validatePending(proposal);

//        Post post = findPost(proposal.getPostId());
        Post post = postService.findById(proposal.getPostId());
        validatePostAuthor(memberId, post);

        proposal.reject();

        return ProposalResponseDto.from(proposal);
    }

    /*
    제안 철회
    - PENDING 상태만 철회 가능
    - 제안한 시터 본인만 철회 가능
     */
    @Transactional
    public ProposalResponseDto withdraw(Long memberId, Long proposalId) {
        Proposal proposal = findById(proposalId);
        validatePending(proposal);

        SitterProfile sitter = sitterService.findByMemberId(memberId);
        validateProposalOwner(sitter.getId(), proposal);

        proposal.withdraw();

        return ProposalResponseDto.from(proposal);
    }

    // ===== transaction 아닌거 =====

    public Proposal findById(Long proposalId) {
        return proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ProposalException(ProposalErrorCode.PROPOSAL_NOT_FOUND));
    }

    /*
    PostService에서 사용 — 공고 수정 시 PENDING/ACCEPTED Proposal 존재 여부 확인
     */
    public boolean existsPendingOrAcceptedByPostId(Long postId) {
        return proposalRepository.existsByPostIdAndStatusIn(
                postId, List.of(ProposalStatus.PENDING, ProposalStatus.ACCEPTED));
    }

    private void validatePostOpen(Post post) {
        if (!post.isOpen()) {
            throw new PostException(PostErrorCode.POST_NOT_OPEN);
        }
    }

    private void validateNotOwnPost(Long memberId, Post post) {
        if (post.isOwnedBy(memberId)) {
            throw new ProposalException(ProposalErrorCode.CANNOT_PROPOSE_OWN_POST);
        }
    }

    /*
    이거 Duplicated 조건에 PENDING 상태인 이미 보낸 제안이 있으면 불가능으로 하는게 좋지 않을까...
    잘못 보낸 제안이 있으면 취소하고 다시 넣을 수 있으면 좋으니까 -> 수정 안 만들어도 됨
     */
    private void validateNoDuplicate(Long postId, Long sitterProfileId) {
        if (proposalRepository.existsByPostIdAndSitterProfileIdAndStatus(postId, sitterProfileId, ProposalStatus.PENDING)) {
            throw new ProposalException(ProposalErrorCode.DUPLICATE_PROPOSAL);
        }
    }

    private void validatePending(Proposal proposal) {
        if (!proposal.isPending()) {
            throw new ProposalException(ProposalErrorCode.NOT_PENDING_PROPOSAL);
        }
    }

    private void validatePostAuthor(Long memberId, Post post) {
        if (!post.isOwnedBy(memberId)) {
            throw new PostException(PostErrorCode.NOT_POST_AUTHOR);
        }
    }

    /*
    제안 당사자 검증
    공고 작성자 또는 제안한 시터만 접근 가능
     */
    private void validateParty(Long memberId, Proposal proposal) {
//        Post post = findPost(proposal.getPostId());
        Post post = postService.findById(proposal.getPostId());


        if (!post.isOwnedBy(memberId) && !proposal.getSitterMemberId().equals(memberId)) {
            throw new ProposalException(ProposalErrorCode.NOT_PROPOSAL_PARTY);
        }
    }

    private void validateProposalOwner(Long sitterProfileId, Proposal proposal) {
        if (!proposal.isOwnedBySitter(sitterProfileId)) {
            throw new ProposalException(ProposalErrorCode.NOT_PROPOSAL_PARTY);
        }
    }

    /*
    같은 공고의 나머지 PENDING 제안을 REJECTED로 일괄 변경

    Reservation 확정 이벤트 발행 시 호출하기
     */
    private void rejectRemainingProposals(Long postId, Long acceptedProposalId) {
        proposalRepository.findAllByPostIdAndStatus(postId, ProposalStatus.PENDING).stream()
                .filter(p -> !p.getId().equals(acceptedProposalId))
                .forEach(Proposal::reject);
    }

    /*
    PostService 와 ProposalService 사이에 순환참조 발생
    -> ProposalService 에서 PostRepository 를 직접 참조하도록 함
     */
//    private Post findPost(Long postId) {
//        return postRepository.findById(postId)
//                .orElseThrow(() -> new BusinessException(CommonErrorCode.POST_NOT_FOUND));
//    }

    private void validateApproved(SitterProfile sitter) {
        if (!sitter.isApproved()) throw new SitterException(SitterErrorCode.INVALID_SITTER_STATUS);
    }
}
