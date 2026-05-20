package com.forpets.domain.pet.service;

import com.forpets.domain.pet.dto.CreatePetRequest;
import com.forpets.domain.pet.dto.PetResponseDto;
import com.forpets.domain.pet.dto.UpdatePetRequest;
import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.exception.PetErrorCode;
import com.forpets.domain.pet.exception.PetException;
import com.forpets.domain.pet.repository.PetRepository;
import com.forpets.domain.reservation.service.ReservationService;
import com.forpets.global.common.AssociationChecker;
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

    private static final int MAX_PET_COUNT = 10;

    private final PetRepository petRepository;
    private final AssociationChecker associationChecker;

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

    public List<PetResponseDto> getMyPets(Long memberId) {
        return petRepository.findAllByMemberId(memberId).stream()
                .map(PetResponseDto::from)
                .toList();
    }

    public PetResponseDto getById(Long memberId, Long petId) {
        Pet pet = findById(petId);
        validateOwner(memberId, pet);
        return PetResponseDto.from(pet);
    }

    @Transactional
    public PetResponseDto update(Long memberId, Long petId, UpdatePetRequest request) {
        Pet pet = findById(petId);
        validateOwner(memberId, pet);

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
        if (associationChecker.hasPetActiveAssociation(petId)){
            throw new PetException(PetErrorCode.PET_USED_IN_ACTIVE_PROCESS);
        }

        pet.delete();
    }

    public Pet findById(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new PetException(PetErrorCode.PET_NOT_FOUND));
    }

    private void validateOwner(Long memberId, Pet pet) {
        if (!pet.getMemberId().equals(memberId)) {
            throw new PetException(PetErrorCode.NOT_PET_OWNER);
        }
    }

    private void validatePetLimit(Long memberId) {
        long count = petRepository.countByMemberId(memberId);
        if (count >= MAX_PET_COUNT) {
            throw new PetException(PetErrorCode.PET_LIMIT_EXCEEDED);
        }
    }

//    private boolean hasActiveReservation(Long petId) {
//        return reservationService.existsActiveReservationByPetId(petId);
//    }
//
//    private void validateCoreFieldNotChanged(Pet pet, UpdatePetRequest request) {
//        if (pet.getSpecies() != request.species() || pet.getSize() != request.size()) {
//            throw new PetException(PetErrorCode.PET_CORE_FIELD_CHANGE_RESTRICTED);
//        }
//    }

//    private void validateDeletable(Long petId) {
//        if (reservationService.existsActiveReservationByPetId(petId)) {
//            throw new PetException(PetErrorCode.PET_USED_IN_ACTIVE_RESERVATION);
//        }
//    }
}