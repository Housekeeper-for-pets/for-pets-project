package com.forpets.domain.carerequest.dto;

import com.forpets.global.common.CareType;
import com.forpets.global.embed.dto.TimeSlotRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateCareRequestDto(
        @NotEmpty(message = "반려동물을 최소 1마리 이상 선택해야 합니다")
        List<Long> petIds,

        @NotNull(message = "돌봄 유형은 필수입니다")
        CareType careType,

        String message,

        @NotEmpty(message = "시간 슬롯을 최소 1개 이상 등록해야 합니다")
        List<@Valid TimeSlotRequest> timeSlots,

        @NotNull(message = "제안 금액은 필수입니다")
        @Positive(message = "제안 금액은 0보다 커야 합니다")
        int requestPrice
) {}
