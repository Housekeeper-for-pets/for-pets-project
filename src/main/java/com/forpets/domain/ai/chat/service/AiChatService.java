package com.forpets.domain.ai.chat.service;

import com.forpets.domain.ai.chat.client.AiChatClient;
import com.forpets.domain.ai.chat.dto.AiChatRequest;
import com.forpets.domain.ai.chat.dto.AiChatResponse;
import com.forpets.domain.ai.chat.dto.AiChatSessionMessage;
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
    private final AiChatSessionStore aiChatSessionStore;

    public AiChatResponse chat(Long memberId, AiChatRequest request) {
        String sessionId = aiChatSessionStore.resolveSessionId(request.sessionId());
        List<AiChatSessionMessage> history = aiChatSessionStore.loadRecentMessages(memberId, sessionId);
        List<RagSearchResultDto> sources = aiRagService.searchSources(request.message());
        AiChatResponse response = aiChatClient.chatWithTools(buildContextMessage(history, request.message()), sitterRecommendationTool);
        String answer = appendSourceNote(response.answer(), sources);

        aiChatSessionStore.appendTurn(memberId, sessionId, request.message(), answer);

        return new AiChatResponse(sessionId, answer, response.recommendedSitters(), sources);
    }

    private String buildContextMessage(List<AiChatSessionMessage> history, String currentMessage) {
        if (history.isEmpty()) {
            return currentMessage;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("아래는 같은 사용자의 최근 AI 시터 추천 대화입니다.\n");
        builder.append("이전 맥락은 참고하되, 실제 추천은 반드시 Tool Calling 결과에 근거하세요.\n\n");
        builder.append("[최근 대화]\n");
        for (AiChatSessionMessage message : history) {
            builder.append(message.role()).append(": ").append(message.content()).append("\n");
        }
        builder.append("\n[현재 요청]\n").append(currentMessage);
        return builder.toString();
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
