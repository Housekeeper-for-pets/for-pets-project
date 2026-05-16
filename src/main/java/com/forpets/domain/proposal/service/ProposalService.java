package com.forpets.domain.proposal.service;

import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.domain.post.service.PostService;
import com.forpets.domain.proposal.dto.CreateProposalRequest;
import com.forpets.domain.proposal.dto.ProposalResponseDto;
import com.forpets.domain.proposal.entity.Proposal;
import com.forpets.domain.proposal.entity.ProposalStatus;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.service.SitterService;
import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final PostService postService;
    private final SitterService sitterService;



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
        validateNoReservationConflict(sitter.getId(), postId);

        Proposal proposal = proposalRepository.save(Proposal.builder()
                .postId(postId)
                .sitterProfileId(sitter.getId())
                .memberId(memberId)
                .proposedPrice(request.proposedPrice())
                .message(request.message())
                .build());

        return ProposalResponseDto.from(proposal);
    }

    /*
    공고에 들어온 제안 목록 조회
    공고 작성자만 조회 가능
     */
    public List<ProposalResponseDto> getByPostId(Long memberId, Long postId) {
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
    7. TODO: Reservation 자동 생성 (PENDING)
       - PostPet → ReservationPet 스냅샷 복사
       - PostTimeSlot → ReservationTimeSlot 복사
     */
    @Transactional
    public ProposalResponseDto accept(Long memberId, Long proposalId) {
        Proposal proposal = findById(proposalId);
        validatePending(proposal);

        Post post = postService.findById(proposal.getPostId());
        validatePostAuthor(memberId, post);
        validatePostOpen(post);

        // 제안 채택
        proposal.accept();

        // 같은 공고의 나머지 PENDING 제안 → REJECTED
        rejectRemainingProposals(post.getId(), proposalId);

        // 공고 상태 → CLOSED
        post.close();

        // TODO: Reservation 자동 생성
        // 동기 처리 vs EventListener 방식 결정 후 구현
        // List<PostPet> postPets = postService.findPetsByPostId(post.getId());
        // List<PostTimeSlot> postTimeSlots = postService.findTimeSlotsByPostId(post.getId());
        // reservationService.createFromProposal(proposal, post, postPets, postTimeSlots);

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
                .orElseThrow(() -> new BusinessException(CommonErrorCode.PROPOSAL_NOT_FOUND));
    }

    /*
    PostService에서 사용 — 공고 수정 시 PENDING/ACCEPTED Proposal 존재 여부 확인
     */
    public boolean existsPendingOrAcceptedByPostId(Long postId) {
        return proposalRepository.existsByPostIdAndStatusIn(
                postId, List.of(ProposalStatus.PENDING, ProposalStatus.ACCEPTED));
    }

    /*
    PostService에서 사용 — 공고 상태 변경/삭제 시 ACCEPTED Proposal 존재 여부 확인
     */
    public boolean existsAcceptedByPostId(Long postId) {
        return proposalRepository.existsByPostIdAndStatus(postId, ProposalStatus.ACCEPTED);
    }

    private void validatePostOpen(Post post) {
        if (!post.isOpen()) {
            throw new BusinessException(CommonErrorCode.POST_NOT_OPEN);
        }
    }

    private void validateNotOwnPost(Long memberId, Post post) {
        if (post.isOwnedBy(memberId)) {
            throw new BusinessException(CommonErrorCode.CANNOT_PROPOSE_OWN_POST);
        }
    }

    private void validateNoDuplicate(Long postId, Long sitterProfileId) {
        if (proposalRepository.existsByPostIdAndSitterProfileId(postId, sitterProfileId)) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_PROPOSAL);
        }
    }

    private void validatePending(Proposal proposal) {
        if (!proposal.isPending()) {
            throw new BusinessException(CommonErrorCode.NOT_PENDING_PROPOSAL);
        }
    }

    private void validatePostAuthor(Long memberId, Post post) {
        if (!post.isOwnedBy(memberId)) {
            throw new BusinessException(CommonErrorCode.NOT_POST_AUTHOR);
        }
    }

    /*
    제안 당사자 검증
    공고 작성자 또는 제안한 시터만 접근 가능
     */
    private void validateParty(Long memberId, Proposal proposal) {
        boolean isPostAuthor = false;
        try {
            Post post = postService.findById(proposal.getPostId());
            isPostAuthor = post.isOwnedBy(memberId);
        } catch (BusinessException ignored) {
        }

        boolean isProposalSitter = proposal.getMemberId().equals(memberId);

        if (!isPostAuthor && !isProposalSitter) {
            throw new BusinessException(CommonErrorCode.NOT_PROPOSAL_PARTY);
        }
    }

    private void validateProposalOwner(Long sitterProfileId, Proposal proposal) {
        if (!proposal.isOwnedBySitter(sitterProfileId)) {
            throw new BusinessException(CommonErrorCode.NOT_PROPOSAL_PARTY);
        }
    }

    /*
    CONFIRMED 예약과 시간 충돌 검증
    TODO: Reservation 도메인 구현 후 연동
    시터의 CONFIRMED 예약 시간과 공고의 TimeSlot이 겹치는지 확인
     */
    private void validateNoReservationConflict(Long sitterProfileId, Long postId) {
        // List<PostTimeSlot> postTimeSlots = postService.findTimeSlotsByPostId(postId);
        // if (reservationService.hasConflict(sitterProfileId, postTimeSlots)) {
        //     throw new BusinessException(CommonErrorCode.RESERVATION_CONFLICT);
        // }
    }

    /*
    같은 공고의 나머지 PENDING 제안을 REJECTED로 일괄 변경
     */
    private void rejectRemainingProposals(Long postId, Long acceptedProposalId) {
        proposalRepository.findAllByPostIdAndStatus(postId, ProposalStatus.PENDING).stream()
                .filter(p -> !p.getId().equals(acceptedProposalId))
                .forEach(Proposal::reject);
    }
}
