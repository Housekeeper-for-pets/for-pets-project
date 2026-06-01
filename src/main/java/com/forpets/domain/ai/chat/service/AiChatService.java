package com.forpets.domain.ai.chat.service;

import com.forpets.domain.ai.chat.client.AiChatClient;
import com.forpets.domain.ai.chat.dto.AiChatRequest;
import com.forpets.domain.ai.chat.dto.AiChatResponse;
import com.forpets.domain.ai.chat.dto.AiSitterSearchCondition;
import com.forpets.domain.ai.chat.dto.RecommendedSitterDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiChatService {

    private final AiChatClient aiChatClient;
    private final AiSitterRecommendationTool sitterRecommendationTool;

    public AiChatResponse chat(Long memberId, AiChatRequest request) {
        AiSitterSearchCondition condition = aiChatClient.extractCondition(request.message());
        List<RecommendedSitterDto> candidates = sitterRecommendationTool.searchSitters(condition);
        String answer = aiChatClient.generateAnswer(request.message(), condition, candidates);

        return new AiChatResponse(answer, candidates);
    }
}
