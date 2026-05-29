package com.forpets.domain.post.service;


import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.exception.PetErrorCode;
import com.forpets.domain.pet.exception.PetException;
import com.forpets.domain.pet.service.PetService;
import com.forpets.domain.post.dto.CreatePostRequest;
import com.forpets.domain.post.dto.PostPageResponse;
import com.forpets.domain.post.dto.PostResponseDto;
import com.forpets.domain.post.dto.PostSearchCondition;
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
import com.forpets.global.embed.TimeSlotValidator;
import com.forpets.global.embed.dto.TimeSlotRequest;
import com.forpets.global.embed.entity.TimeSlotInfo;
import com.forpets.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostPetRepository postPetRepository;
    private final PostTimeSlotRepository postTimeSlotRepository;
    private final PetService petService;
    private final TimeSlotValidator timeSlotValidator;
    private final MemberService memberService;
    private final ProposalRepository proposalRepository;
    private final PostCacheService postCacheService;


    /*
    공고 등록
    1. petIds → Pet 존재 여부 + 본인 소유 검증
    2. TimeSlotValidator 검증 (최대 30개, 과거 날짜, 시간 역전, 겹침)
    3. Post 생성 (초기 상태: OPEN)
    4. PostPet 생성 (PetSnapshot 저장)
    5. PostTimeSlot 생성 (sequence 자동 계산: careDate ASC, startTime ASC)
     */
    @Transactional
    @CacheEvict(cacheNames = "postings", allEntries = true, cacheManager = "shortTtlCacheManager")
    public PostResponseDto create(Long memberId, CreatePostRequest request) {
        Member member = memberService.findById(memberId);
        List<Pet> pets = validateAndGetPets(memberId, request.petIds());
        timeSlotValidator.validate(request.timeSlots());

        Post post = postRepository.save(Post.builder()
                .memberId(memberId)
                .title(request.title())
                .content(request.content())
                .careType(request.careType())
                .budgetAmount(request.budgetAmount())
                .build());

        List<PostPet> postPets = savePostPets(post.getId(), pets);
        List<PostTimeSlot> postTimeSlots = savePostTimeSlots(post.getId(), request.timeSlots());

        return PostResponseDto.from(post, member.getRegion(), postPets, postTimeSlots);
    }

    /*
    공고 수정 (전체 교체 방식)
    수정 조건:
    - OPEN 상태인 공고만 수정 가능
    - PENDING 또는 ACCEPTED Proposal이 하나라도 있으면 수정 불가
    - 본인 공고만 수정 가능

    PostPet, PostTimeSlot은 교체 방식 (전체 삭제 → 신규 삽입)
    PetSnapshot은 수정 시점의 최신 Pet 정보로 재생성
     */
    @Transactional
    @CacheEvict(cacheNames = "postings", allEntries = true, cacheManager = "shortTtlCacheManager")
    public PostResponseDto update(Long memberId, Long postId, UpdatePostRequest request) {
        Member member = memberService.findById(memberId);

        Post post = findById(postId);
        validateAuthor(memberId, post);
        validateOpen(post);
        validateNoActiveProposal(postId);

        List<Pet> pets = validateAndGetPets(memberId, request.petIds());
        timeSlotValidator.validate(request.timeSlots());

        post.update(
                request.title(),
                request.content(),
                request.careType(),
                request.budgetAmount()
        );

        // PostPet 교체 (스냅샷 재생성)
        postPetRepository.deleteAllByPostId(postId);
        postPetRepository.flush();
        List<PostPet> postPets = savePostPets(postId, pets);

        // PostTimeSlot 교체 (sequence 재계산)
        postTimeSlotRepository.deleteAllByPostId(postId);
        postTimeSlotRepository.flush();
        List<PostTimeSlot> postTimeSlots = savePostTimeSlots(postId, request.timeSlots());

        return PostResponseDto.from(post, member.getRegion(), postPets, postTimeSlots);
    }

    /*
    공고 close 상태로 변경하기

    근데 어차피 Close 상태인데 postPet 이랑 postTimeSlot 을 출력해주는 의미가 있나?
    DB 부하 심해질 것 같으면 따로 Dto 만들어서 빼주기
    아니면 void 반환해줘도 될 것 같음 고민해보기
     */
    @Transactional
    @CacheEvict(cacheNames = "postings", allEntries = true, cacheManager = "shortTtlCacheManager")
    public PostResponseDto closePost(Long memberId, Long postId) {
        Member member = memberService.findById(memberId);

        Post post = findById(postId);
        validateAuthor(memberId, post);
        validateNoActiveProposal(postId);

        post.close();

        List<PostPet> postPets = postPetRepository.findAllByPostId(postId);
        List<PostTimeSlot> postTimeSlots = postTimeSlotRepository
                .findAllByPostIdOrderByTimeSlotInfoSequence(postId);

        return PostResponseDto.from(post, member.getRegion(), postPets, postTimeSlots);
    }

    /*
    공고 삭제 (Soft Delete → status = DELETED)
    삭제 가능 조건:
    - OPEN 또는 CLOSED 상태
    - ACCEPTED Proposal이 없을 때만
    - 본인 공고만
     */
    @Transactional
    @CacheEvict(cacheNames = "postings", allEntries = true, cacheManager = "shortTtlCacheManager")
    public void delete(Long memberId, Long postId) {
        Post post = findById(postId);
        validateAuthor(memberId, post);
        validateNoActiveProposal(postId);

        post.delete();
    }

    // ===== 트랜잭션 아닌 애들 =====

    public Post findById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));
    }

    public PostResponseDto getPost(Long postId) {
        Post post = findById(postId);
        Member member = memberService.findById(post.getMemberId());
        List<PostPet> pets = postPetRepository.findAllByPostId(postId);
        List<PostTimeSlot> timeSlots = postTimeSlotRepository
                .findAllByPostIdOrderByTimeSlotInfoSequence(postId);
        return PostResponseDto.from(post, member.getRegion(), pets, timeSlots);
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "budgetAmount"
    );

    private void validateSortField(String sort) {
        if (!ALLOWED_SORT_FIELDS.contains(sort)) {
            throw new BusinessException(PostErrorCode.INVALID_SORT_FIELD);
        }
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new BusinessException(PostErrorCode.INVALID_PAGE_REQUEST);
        }
        if (size < 1 || size > 50) {
            throw new BusinessException(PostErrorCode.INVALID_PAGE_REQUEST);
        }
    }

    /*
    공고 목록 검색 (GET /api/posts)
    - 정렬 필드 화이트리스트 검증 (createdAt / updatedAt / budgetAmount)
    - 페이지 크기 최대 50 제한
    - 페이지 번호 음수 불가
     */
    public PostPageResponse searchPosts(PostSearchCondition condition, int page, int size, String sort) {
        validatePageRequest(page, size);
        validateSortField(sort);

        return postCacheService.searchPostings(condition, page, size, sort);
    }

    public PostPageResponse searchMyPosts(Long memberId, String status, int page, int size) {
        validatePageRequest(page, size);
        PostStatus postStatus = parseStatus(status);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return postRepository.searchMyPosts(memberId, postStatus, pageable);
    }

    private PostStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return PostStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new PostException(PostErrorCode.INVALID_POST_STATUS);
        }
    }

    public List<PostTimeSlot> findTimeSlotsByPostId(Long postId) {
        return postTimeSlotRepository.findAllByPostIdOrderByTimeSlotInfoSequence(postId);
    }

    public List<PostPet> findPetsByPostId(Long postId) {
        return postPetRepository.findAllByPostId(postId);
    }

    /*
    반려동물 존재 여부 + 본인 소유 검증
    PetService.findById로 존재 확인 후 본인 소유 검증
     */
    private List<Pet> validateAndGetPets(Long memberId, List<Long> petIds) {
        return petIds.stream()
                .map(petId -> {
                    Pet pet = petService.findById(petId);
                    if (!pet.getMemberId().equals(memberId)) {
                        throw new PetException(PetErrorCode.NOT_PET_OWNER);
                    }
                    return pet;
                })
                .toList();
    }

    private void validateAuthor(Long memberId, Post post) {
        if (!post.isOwnedBy(memberId)) {
            throw new PostException(PostErrorCode.NOT_POST_AUTHOR);
        }
    }

    private void validateOpen(Post post) {
        if (!post.isOpen()) {
            throw new PostException(PostErrorCode.POST_NOT_OPEN);
        }
    }

    private void validateNoActiveProposal(Long postId) {
        if (proposalRepository.existsByPostIdAndStatusIn(
                postId, List.of(ProposalStatus.PENDING, ProposalStatus.ACCEPTED))) {
            throw new ProposalException(ProposalErrorCode.HAS_ACTIVE_PROPOSAL);
        }
    }

    /*
    PostPet 저장 — Pet 원본에서 PetSnapshot 생성
     */
    private List<PostPet> savePostPets(Long postId, List<Pet> pets) {
        List<PostPet> postPets = pets.stream()
                .map(pet -> PostPet.createFrom(postId, pet))
                .toList();
        return postPetRepository.saveAll(postPets);
    }

    /*
    PostTimeSlot 저장 + sequence 자동 계산
    정렬: careDate ASC → startTime ASC
    sequence: 1부터 순차 할당
    클라이언트가 보낸 sequence 값은 무시
     */
    private List<PostTimeSlot> savePostTimeSlots(Long postId, List<TimeSlotRequest> timeSlots) {
        List<TimeSlotRequest> sorted = timeSlots.stream()
                .sorted(Comparator.comparing(TimeSlotRequest::careDate)
                        .thenComparing(TimeSlotRequest::startTime))
                .toList();

        List<PostTimeSlot> postTimeSlots = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            TimeSlotRequest slot = sorted.get(i);
            TimeSlotInfo info = TimeSlotInfo.of(
                    slot.careDate(), slot.startTime(), slot.endTime(), i + 1);
            postTimeSlots.add(PostTimeSlot.create(postId, info));
        }

        return postTimeSlotRepository.saveAll(postTimeSlots);
    }
}
