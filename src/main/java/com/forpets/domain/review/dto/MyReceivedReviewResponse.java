package com.forpets.domain.review.dto;

import java.time.LocalDateTime;

public record MyReceivedReviewResponse(
        Long id,
        Long reservationId,
        Long reviewerId,
        String reviewerNickname,
        Integer rating,
        String reviewComment,
        LocalDateTime createdAt
) {
}
