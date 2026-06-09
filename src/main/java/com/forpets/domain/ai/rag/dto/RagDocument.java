package com.forpets.domain.ai.rag.dto;

public record RagDocument(
        String pointId,
        Long sourceId,
        Long sitterId,
        Integer rating,
        String content,
        RagSourceType sourceType
) {
}
