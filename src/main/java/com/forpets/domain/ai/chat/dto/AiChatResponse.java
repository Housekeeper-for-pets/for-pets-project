package com.forpets.domain.ai.chat.dto;

import com.forpets.domain.ai.rag.dto.RagSearchResultDto;

import java.util.List;

public record AiChatResponse(
        String sessionId,
        String answer,
        List<RecommendedSitterDto> recommendedSitters,
        List<RagSearchResultDto> sources
) {

    public AiChatResponse(String answer, List<RecommendedSitterDto> recommendedSitters) {
        this(null, answer, recommendedSitters, List.of());
    }

    public AiChatResponse(String answer, List<RecommendedSitterDto> recommendedSitters, List<RagSearchResultDto> sources) {
        this(null, answer, recommendedSitters, sources);
    }
}
