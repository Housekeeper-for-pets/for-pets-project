package com.forpets.domain.ai.rag.dto;

public record RagDocument(
        Long reviewId,
        Long sitterId,
        Integer rating,
        String content,
        RagSourceType sourceType
) {
}
