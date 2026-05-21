package com.forpets.domain.pet.dto;

import com.forpets.domain.pet.entity.PetGender;
import com.forpets.domain.pet.entity.PetSize;
import com.forpets.domain.pet.entity.PetSpecies;
import jakarta.validation.constraints.*;

public record UpdatePetRequest(
        @NotBlank(message = "반려동물 이름은 필수입니다")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다")
        String name,

        @NotNull(message = "반려동물 종류는 필수입니다")
        PetSpecies species,

        @Size(max = 50, message = "품종은 50자 이하여야 합니다")
        String breed,

        PetSize size,

        @Min(value = 0, message = "나이는 0 이상이어야 합니다")
        @Max(value = 100, message = "나이는 100 이하여야 합니다")
        Integer age,

        PetGender gender,

        @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다")
        String profileImageUrl,

        @Size(max = 2000, message = "메모는 2000자 이하여야 합니다")
        String note
) {}
