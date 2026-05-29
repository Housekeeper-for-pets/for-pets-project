package com.forpets.domain.ai.reviewsummary.client;

import com.forpets.domain.ai.reviewsummary.dto.SitterReviewSummaryResponse;

public interface AiReviewSummaryClient {

    AiReviewSummaryResult generate(String prompt);

    record AiReviewSummaryResult(
            SitterReviewSummaryResponse response,
            String model
    ) {
    }
}
