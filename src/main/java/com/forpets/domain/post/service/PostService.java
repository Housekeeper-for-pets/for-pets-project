package com.forpets.domain.post.service;


import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.service.PetService;
import com.forpets.domain.post.dto.CreatePostRequest;
import com.forpets.domain.post.dto.PostResponseDto;
import com.forpets.domain.post.dto.UpdatePostRequest;
import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostPet;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.domain.post.entity.PostTimeSlot;
import com.forpets.domain.post.repository.PostPetRepository;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.post.repository.PostTimeSlotRepository;
import com.forpets.global.embed.TimeSlotValidator;
import com.forpets.global.embed.dto.TimeSlotRequest;
import com.forpets.global.embed.entity.TimeSlotInfo;
import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostPetRepository postPetRepository;
    private final PostTimeSlotRepository postTimeSlotRepository;
    private final PetService petService;
    private final TimeSlotValidator timeSlotValidator;


    /*
    공고 등록
    1. petIds → Pet 존재 여부 + 본인 소유 검증
    2. TimeSlotValidator 검증 (최대 30개, 과거 날짜, 시간 역전, 겹침)
    3. Post 생성 (초기 상태: OPEN)
    4. PostPet 생성 (PetSnapshot 저장)
    5. PostTimeSlot 생성 (sequence 자동 계산: careDate ASC, startTime ASC)
     */
    @Transactional
    public PostResponseDto create(Long memberId, CreatePostRequest request) {
        List<Pet> pets = validateAndGetPets(memberId, request.petIds());
        timeSlotValidator.validate(request.timeSlots());

        Post post = postRepository.save(Post.builder()
                .memberId(memberId)
                .title(request.title())
                .content(request.content())
                .region(request.region())
                .careType(request.careType())
                .budgetAmount(request.budgetAmount())
                .build());

        List<PostPet> postPets = savePostPets(post.getId(), pets);
        List<PostTimeSlot> postTimeSlots = savePostTimeSlots(post.getId(), request.timeSlots());

        return PostResponseDto.from(post, postPets, postTimeSlots);
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
    public PostResponseDto update(Long memberId, Long postId, UpdatePostRequest request) {
        Post post = findById(postId);
        validateAuthor(memberId, post);
        validateOpen(post);
        validateNoActiveProposal(postId);

        List<Pet> pets = validateAndGetPets(memberId, request.petIds());
        timeSlotValidator.validate(request.timeSlots());

        post.update(
                request.title(),
                request.content(),
                request.region(),
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

        return PostResponseDto.from(post, postPets, postTimeSlots);
    }

    /*
    공고 close 상태로 변경하기

    근데 어차피 Close 상태인데 postPet 이랑 postTimeSlot 을 출력해주는 의미가 있나?
    DB 부하 심해질 것 같으면 따로 Dto 만들어서 빼주기
    아니면 void 반환해줘도 될 것 같음 고민해보기
     */
    @Transactional
    public PostResponseDto closePost(Long memberId, Long postId) {
        Post post = findById(postId);
        validateAuthor(memberId, post);
        validateNoActiveProposal(postId);

        post.close();

        List<PostPet> postPets = postPetRepository.findAllByPostId(postId);
        List<PostTimeSlot> postTimeSlots = postTimeSlotRepository
                .findAllByPostIdOrderByTimeSlotInfoSequence(postId);

        return PostResponseDto.from(post, postPets, postTimeSlots);
    }

    /*
    공고 삭제 (Soft Delete → status = DELETED)
    삭제 가능 조건:
    - OPEN 또는 CLOSED 상태
    - ACCEPTED Proposal이 없을 때만
    - 본인 공고만
     */
    @Transactional
    public void delete(Long memberId, Long postId) {
        Post post = findById(postId);
        validateAuthor(memberId, post);
        validateNoActiveProposal(postId);

        post.delete();
    }

    // ===== 트랜잭션 아닌 애들 =====

    public Post findById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.POST_NOT_FOUND));
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
                        throw new BusinessException(CommonErrorCode.NOT_PET_OWNER);
                    }
                    return pet;
                })
                .toList();
    }

    private void validateAuthor(Long memberId, Post post) {
        if (!post.isOwnedBy(memberId)) {
            throw new BusinessException(CommonErrorCode.NOT_POST_AUTHOR);
        }
    }

    private void validateOpen(Post post) {
        if (!post.isOpen()) {
            throw new BusinessException(CommonErrorCode.POST_NOT_OPEN);
        }
    }

    /*
    PENDING 또는 ACCEPTED Proposal이 있으면 수정, 상태변경, 삭제 불가

    MVP 정책이 원래 PENDING Proposal만 있으면 상태 변경, 삭제 가능이었는데
    그냥 구분하지 않고 모든 요청을 다 REJECT 처리 다 해야 상태 변경, 삭제 가능하도록 하는게 깔끔할듯
     */
    private void validateNoActiveProposal(Long postId) {
        // if (proposalService.existsPendingOrAcceptedByPostId(postId)) {
        //     throw new BusinessException(CommonErrorCode.HAS_PENDING_PROPOSAL);
        // }
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

    public List<Post> getTest() {
        return postRepository.findAll();
    }
}