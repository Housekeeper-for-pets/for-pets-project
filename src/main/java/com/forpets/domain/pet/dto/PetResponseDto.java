package com.forpets.domain.pet.dto;

import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.entity.PetGender;
import com.forpets.domain.pet.entity.PetSize;
import com.forpets.domain.pet.entity.PetSpecies;

import java.time.LocalDateTime;

public record PetResponseDto(
        Long id,
        Long memberId,
        String name,
        PetSpecies species,
        String breed,
        PetSize size,
        Integer age,
        PetGender gender,
        String profileImageUrl,
        String note,
        LocalDateTime createdAt
) {
    public static PetResponseDto from(Pet pet) {
        return new PetResponseDto(
                pet.getId(),
                pet.getMemberId(),
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getSize(),
                pet.getAge(),
                pet.getGender(),
                pet.getProfileImageUrl(),
                pet.getNote(),
                pet.getCreatedAt()
        );
    }
}
