package com.forpets.domain.post.dto;

import com.forpets.global.common.CareType;
import com.forpets.global.embed.dto.TimeSlotRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record UpdatePostRequest(
        @NotBlank(message = "공고 제목은 필수입니다")
        String title,

        @NotBlank(message = "공고 내용은 필수입니다")
        String content,

        @NotEmpty(message = "반려동물을 최소 1마리 이상 선택해야 합니다")
        List<Long> petIds,

        @NotBlank(message = "희망 지역은 필수입니다")
        String region,

        @NotNull(message = "돌봄 유형은 필수입니다")
        CareType careType,

        @Positive(message = "희망 예산은 0보다 커야 합니다")
        Integer budgetAmount,

        @NotEmpty(message = "시간 슬롯을 최소 1개 이상 등록해야 합니다")
        List<@Valid TimeSlotRequest> timeSlots
) {}