package com.forpets.domain.pet.dto;

import com.forpets.domain.pet.entity.PetGender;
import com.forpets.domain.pet.entity.PetSize;
import com.forpets.domain.pet.entity.PetSpecies;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePetRequest(
        @NotBlank(message = "반려동물 이름은 필수입니다")
        String name,

        @NotNull(message = "반려동물 종류는 필수입니다")
        PetSpecies species,

        String breed,
        PetSize size,
        Integer age,
        PetGender gender,
        String profileImageUrl,
        String note
) {}
