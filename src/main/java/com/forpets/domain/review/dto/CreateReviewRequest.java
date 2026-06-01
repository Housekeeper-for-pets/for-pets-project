package com.forpets.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotNull(message = "예약 ID는 필수입니다.")
        Long reservationId,

        @NotBlank(message = "리뷰 내용은 필수입니다.")
        @Size(min = 10, max = 500, message = "리뷰 내용은 10자 이상 500자 이하로 입력해주세요.")
        String reviewComment,

        @NotNull(message = "평점은 필수입니다.")
        @Min(value = 1, message = "평점은 1점 이상이어야 합니다.")
        @Max(value = 5, message = "평점은 5점 이하이어야 합니다.")
        Integer rating
) {
}
