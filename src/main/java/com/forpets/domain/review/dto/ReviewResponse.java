package com.forpets.domain.review.dto;

import com.forpets.domain.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long reservationId,
        Long reviewerId,
        Long revieweeId,
        String reviewComment,
        Integer rating,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getReservationId(),
                review.getReviewerId(),
                review.getRevieweeId(),
                review.getReviewComment(),
                review.getRating(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
