package com.forpets.domain.pet.service;

import com.forpets.domain.pet.dto.CreatePetRequest;
import com.forpets.domain.pet.dto.PetResponseDto;
import com.forpets.domain.pet.dto.UpdatePetRequest;
import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.repository.PetRepository;
import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    /*
    정책에서 반려동물의 최대 Count 수를 10으로 설정
    -> 반려동물을 등록 할 때 수를 초과하지 않았는지 확인하는 로직에서 사용하는 정적값
     */
    private static final int MAX_PET_COUNT = 10;

    private final PetRepository petRepository;

    @Transactional
    public PetResponseDto create(Long memberId, CreatePetRequest request) {
        validatePetLimit(memberId);

        Pet pet = Pet.builder()
                .memberId(memberId)
                .name(request.name())
                .species(request.species())
                .breed(request.breed())
                .size(request.size())
                .age(request.age())
                .gender(request.gender())
                .profileImageUrl(request.profileImageUrl())
                .note(request.note())
                .build();

        return PetResponseDto.from(petRepository.save(pet));
    }

    /*
    목록 조회지만 최대 10개의 Colume 이 출력되기 때문에 굳이 Paging 처리가 필요없다고 판단
     */
    public List<PetResponseDto> getMyPets(Long memberId) {
        return petRepository.findAllByMemberId(memberId).stream()
                .map(PetResponseDto::from)
                .toList();
    }

    /*
    반려동물의 상세 정보를 조회합니다.
    본인 소유의 반려동물 (pet.getMemeberId 가 로그인 한 사용자의 ID (memberID) 와 동일해야합니다.
     */
    public PetResponseDto getById(Long memberId, Long petId) {
        Pet pet = findById(petId);
        validateOwner(memberId, pet);
        return PetResponseDto.from(pet);
    }

    /*
    반려동물 정보를 수정합니다.
    본인 소유의 반려동물 (pet.getMemeberId 가 로그인 한 사용자의 ID (memberID) 와 동일해야합니다.

    전체 교체 방식을 사용
    프론트에서 수정 화면을 열 때 기존 데이터를 전부 넣어두고 수정한 필드만 바꿔서 전체 필드를 UpdateRequest 로 보냄

    Patch 로 바뀐 필드만 보내는 형식을 사용하지 않은 이유
    1. service 단에서 특정 값이 null 이면 안 바꾸고 값이 있으면 바꾸는 분기 처리가 복잡해짐
    2. Pet Entity 에 nullable field 가 많음
        -> 해당 값을 null 로 업데이트? vs 아예 안 보낸건지 구분 힘듦
    3. 실무에서 더 많이 쓰는 방식
     */
    @Transactional
    public PetResponseDto update(Long memberId, Long petId, UpdatePetRequest request) {
        Pet pet = findById(petId);
        validateOwner(memberId, pet);

//        if (hasActiveReservation(petId)){
//            isCoreFieldChanged(pet, request);
//        }

        pet.update(
                request.name(),
                request.species(),
                request.breed(),
                request.size(),
                request.age(),
                request.gender(),
                request.profileImageUrl(),
                request.note()
        );

        return PetResponseDto.from(pet);
    }

    @Transactional
    public void delete(Long memberId, Long petId) {
        Pet pet = findById(petId);
        validateOwner(memberId, pet);
//        validateDeletable(petId);

        pet.delete();
    }

    // --- transactional 이 아닌 methods ---

    /*
    pet Id 를 입력받아 Pet Entity 를 return 합니다.
    pet Id 가 존재하지 않거나, Soft Deleted 된 상태라면 PET_NOT_FOUND error 를 throw 합니다.

    타 도메인에서 Pet Entity 에 접근할 때 사용할 수 있습니다.
    ex) Pet pet = petService.findById(id);
     */
    public Pet findById(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.PET_NOT_FOUND));
    }

    /*
    member Id 와 PetEntity 를 입력받습니다.
    pet 의 주인 Id 와 member Id 가 동일하지 않다면 NOT_PET_OWNER error 를 throw 합니다.
     */
    private void validateOwner(Long memberId, Pet pet) {
        if (!pet.getMemberId().equals(memberId)) {
            throw new BusinessException(CommonErrorCode.NOT_PET_OWNER);
        }
    }

    private void validatePetLimit(Long memberId) {
        long count = petRepository.countByMemberId(memberId);
        if (count >= MAX_PET_COUNT) {
            throw new BusinessException(CommonErrorCode.PET_LIMIT_EXCEEDED);
        }
    }

//    /*
//    예약이 PENDING 또는 CONFIRMED 상태 일 때
//    보호자가 해당 동물의 주요 정보를 수정하는 시나리오 방지
//    ex) 예약 생성은 아기고양이, 2kg 으로 잡고 결제 이후 갑자기 골든리트리버, 30kg 으로 수정하는 경우
//
//    -> PENDING 또는 CONFIRMED 예약이 존재할 때 핵심 필드 수정을 제한
//     */
//    private boolean hasActiveReservation(Long petId){
//        // return reservationService.existsInProgressReservationByPetId(petId)
//        // if 문에 넣어서 CustomException(ErrorCode.PET_USED_IN_ACTIVE_RESERVATION) 던지기
//        return true;
//    }
//
//    /*
//    중요한 정보 (종, 크기 등) 을 수정하려고 하면 PET_CORE_FIELD_CHANGE_RESTRICTED error
//     */
//    private void isCoreFieldChanged(Pet pet, UpdatePetRequest request) {
//        if (pet.getSpecies() != request.species() || pet.getSize() != request.size()) {
//            throw new BusinessException(CommonErrorCode.PET_CORE_FIELD_CHANGE_RESTRICTED);
//        }
//    }
//
//    /*
//    해당 반려동물이 삭제 가능 상태인지 확인하는 로직
//
//    1. Reservation -> PENDING 또는 CONFIRMED 인 경우 삭제 불가능
//    2. Post -> OPEN 상태의 공고에 등록되어 있는 경우 삭제 불가능
//    3. CareRequest -> PENDING 상태의 케어 요청에 포함되어 있는 경우 삭제 불가능
//
//    상태 변경 불가능 상태로 업데이트 이후 삭제가 가능하도록 함
//     */
//    private void validateDeletable(Long petId) {
//        // if (hasActiveReservation(petId)) PET_USED_IN_ACTIVE_RESERVATION
//        // if (postService . . .) PET_USED_IN_OPEN_POST error
//        // if (careRequestService . . .) PET_USED_IN_PENDING_REQUEST error
//    }
}