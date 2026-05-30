package com.forpets.domain.sitter.dto.profile;

import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateSitterRequest(
        String introduction,

        @NotNull(message = "경력 연수는 필수입니다")
        @Min(value = 0, message = "경력 연수를 정확히 입력해주세요")
        Integer experienceYears,

        @NotNull(message = "돌봄 가능한 반려동물 타입은 필수입니다")
        PossiblePetType possiblePetType,

        @NotNull(message = "돌봄 가능한 반려동물 크기는 필수입니다")
        PossiblePetSize possiblePetSize,

        @NotNull(message = "시간당 요금은 필수입니다")
        @Positive(message = "시간당 요금은 0보다 커야 합니다")
        Integer pricePerHour
) {}