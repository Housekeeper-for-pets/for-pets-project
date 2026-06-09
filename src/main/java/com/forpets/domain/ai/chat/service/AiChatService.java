package com.forpets.domain.ai.chat.service;

import com.forpets.domain.ai.chat.client.AiChatClient;
import com.forpets.domain.ai.chat.dto.AiChatRequest;
import com.forpets.domain.ai.chat.dto.AiChatResponse;
import com.forpets.domain.ai.rag.dto.RagSearchResultDto;
import com.forpets.domain.ai.rag.dto.RagSourceType;
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
        List<RagSearchResultDto> sources = aiRagService.searchSources(request.message());
        AiChatResponse response = aiChatClient.chatWithTools(request.message(), sitterRecommendationTool);

        return new AiChatResponse(appendSourceNote(response.answer(), sources), response.recommendedSitters(), sources);
    }

    private String appendSourceNote(String answer, List<RagSearchResultDto> sources) {
        if (sources.isEmpty()) {
            return answer;
        }

        RagSearchResultDto source = sources.get(0);
        String sourceLabel = source.sourceType() == RagSourceType.REVIEW
                ? "리뷰 #" + source.reviewId()
                : "AI 리뷰 요약 #" + source.sourceId();

        return answer + "\n\n리뷰 근거: " + sourceLabel
                + " / 시터 #" + source.sitterId()
                + " / \"" + source.snippet() + "\"";
    }
}
