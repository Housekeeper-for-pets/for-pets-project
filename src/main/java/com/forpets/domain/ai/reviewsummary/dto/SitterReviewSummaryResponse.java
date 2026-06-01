package com.forpets.domain.ai.reviewsummary.dto;

import com.forpets.domain.ai.reviewsummary.entity.ReviewSentiment;

import java.util.List;

public record SitterReviewSummaryResponse(
        String summary,
        List<String> strengths,
        List<String> cautions,
        List<String> recommendedFor,
        List<String> keywords,
        ReviewSentiment sentiment,
        Double confidenceScore,
        Integer reviewCount,
        List<Long> usedReviewIds
) {
}
