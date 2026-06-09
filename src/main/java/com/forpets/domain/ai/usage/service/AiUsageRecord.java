package com.forpets.domain.ai.usage.service;

import com.forpets.domain.ai.usage.entity.*;

public record AiUsageRecord(
        AiFeature feature,
        String model,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        Long latencyMs,
        AiUsageStatus status,
        AiErrorType errorType,
        String promptVersion,
        String failureReason
) {

    public static AiUsageRecord success(
            AiFeature feature,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            Long latencyMs,
            String promptVersion
    ) {
        return new AiUsageRecord(
                feature,
                model,
                promptTokens,
                completionTokens,
                totalTokens,
                latencyMs,
                AiUsageStatus.SUCCESS,
                null,
                promptVersion,
                null
        );
    }

    public static AiUsageRecord fallback(
            AiFeature feature,
            String model,
            Long latencyMs,
            AiErrorType errorType,
            String promptVersion,
            String failureReason
    ) {
        return new AiUsageRecord(
                feature,
                model,
                null,
                null,
                null,
                latencyMs,
                AiUsageStatus.FALLBACK,
                errorType,
                promptVersion,
                failureReason
        );
    }
}
