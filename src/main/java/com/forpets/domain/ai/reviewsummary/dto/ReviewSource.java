package com.forpets.domain.ai.reviewsummary.dto;

public record ReviewSource(
        Long reviewId,
        Integer rating,
        String content
) {
}
