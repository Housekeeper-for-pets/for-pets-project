package com.forpets.domain.review.dto;

import java.time.LocalDateTime;

public record MyWrittenReviewResponse(
        Long id,
        Long reservationId,
        Long revieweeId,
        String revieweeNickname,
        Long sitterProfileId,
        Integer rating,
        String reviewComment,
        LocalDateTime createdAt
) {
}
