package com.forpets.domain.ai.chat.service;

import com.forpets.domain.ai.chat.client.AiChatClient;
import com.forpets.domain.ai.chat.dto.AiChatRequest;
import com.forpets.domain.ai.chat.dto.AiChatResponse;
import com.forpets.domain.ai.rag.dto.RagSearchResultDto;
import com.forpets.domain.ai.rag.service.AiRagService;
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
    private final AiRagService aiRagService;

    public AiChatResponse chat(Long memberId, AiChatRequest request) {
        AiChatResponse response = aiChatClient.chatWithTools(request.message(), sitterRecommendationTool);
        List<RagSearchResultDto> sources = aiRagService.searchSources(request.message());

        return new AiChatResponse(response.answer(), response.recommendedSitters(), sources);
    }
}
