package com.forpets.domain.ai.reviewsummary.client;

import com.forpets.domain.ai.reviewsummary.dto.SitterReviewSummaryResponse;

public interface AiReviewSummaryClient {

    AiReviewSummaryResult generate(String prompt);

    record AiReviewSummaryResult(
            SitterReviewSummaryResponse response,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens
    ) {
        public AiReviewSummaryResult(SitterReviewSummaryResponse response, String model) {
            this(response, model, null, null, null);
        }
    }
}
