package com.forpets.domain.sitter.dto;

import com.forpets.domain.pet.entity.PetSize;
import com.forpets.domain.pet.entity.PetSpecies;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateSitterRequest(
        @NotBlank(message = "활동 지역은 필수입니다")
        String region,

        String introduction,

        @NotNull(message = "경력 연수는 필수입니다")
        Integer experienceYears,

        @NotNull(message = "돌봄 가능한 반려동물 타입은 필수입니다")
        PossiblePetType possiblePetType,

        @NotNull(message = "돌봄 가능한 반려동물 크기는 필수입니다")
        PossiblePetSize possiblePetSize,

        @NotNull(message = "시간당 요금은 필수입니다")
        @Positive(message = "시간당 요금은 0보다 커야 합니다")
        Integer pricePerHour
) {}