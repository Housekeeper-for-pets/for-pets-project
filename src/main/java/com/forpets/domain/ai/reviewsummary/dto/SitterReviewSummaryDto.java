package com.forpets.domain.ai.reviewsummary.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.ai.reviewsummary.entity.SitterReviewSummary;
import com.forpets.domain.ai.reviewsummary.entity.SummaryStatus;

import java.util.List;

public record SitterReviewSummaryDto(
        Long sitterId,
        String summary,
        List<String> strengths,
        List<String> cautions,
        List<String> recommendedFor,
        List<String> keywords,
        String sentiment,
        Double confidenceScore,
        Integer reviewCount,
        Boolean aiGenerated,
        SummaryStatus summaryStatus,
        String model,
        String promptVersion
) {
    public static SitterReviewSummaryDto from(SitterReviewSummary summary, ObjectMapper objectMapper) {
        return new SitterReviewSummaryDto(
                summary.getSitterId(),
                summary.getSummary(),
                readList(objectMapper, summary.getStrengths()),
                readList(objectMapper, summary.getCautions()),
                readList(objectMapper, summary.getRecommendedFor()),
                readList(objectMapper, summary.getKeywords()),
                summary.getSentiment().name(),
                summary.getConfidenceScore(),
                summary.getReviewCount(),
                summary.isAiGenerated(),
                summary.getSummaryStatus(),
                summary.getModel(),
                summary.getPromptVersion()
        );
    }

    private static List<String> readList(ObjectMapper objectMapper, String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }
}
