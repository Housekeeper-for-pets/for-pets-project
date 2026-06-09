package com.forpets.domain.ai.chat.client;

import com.forpets.domain.ai.chat.dto.AiChatResponse;
import com.forpets.domain.ai.chat.dto.AiSitterSearchCondition;
import com.forpets.domain.ai.chat.dto.RecommendedSitterDto;
import com.forpets.domain.ai.chat.service.AiSitterRecommendationTool;

import java.util.List;

public interface AiChatClient {

    AiSitterSearchCondition extractCondition(String message);

    String generateAnswer(String message, AiSitterSearchCondition condition, List<RecommendedSitterDto> candidates);

    default AiChatResponse chatWithTools(String message, AiSitterRecommendationTool sitterRecommendationTool) {
        AiSitterSearchCondition condition = extractCondition(message);
        List<RecommendedSitterDto> candidates = sitterRecommendationTool.buildRecommendations(condition);
        String answer = generateAnswer(message, condition, candidates);

        return new AiChatResponse(answer, candidates);
    }
}
