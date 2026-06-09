package com.forpets.domain.ai.rag.dto;

public record RagSearchResultDto(
        RagSourceType sourceType,
        Long sourceId,
        Long reviewId,
        Long sitterId,
        Integer rating,
        String snippet,
        Double score
) {
}
