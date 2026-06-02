package com.forpets.domain.ai.chat.dto;

import java.util.List;

public record AiChatResponse(
        String answer,
        List<RecommendedSitterDto> recommendedSitters
) {
}
