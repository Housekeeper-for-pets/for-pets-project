package com.forpets.domain.ai.chat.service;

import com.forpets.domain.ai.chat.client.AiChatClient;
import com.forpets.domain.ai.chat.dto.AiChatRequest;
import com.forpets.domain.ai.chat.dto.AiChatResponse;
import com.forpets.domain.ai.chat.dto.AiChatSessionMessage;
import com.forpets.domain.ai.chat.dto.RecommendedSitterDto;
import com.forpets.domain.ai.rag.dto.RagSearchResultDto;
import com.forpets.domain.ai.rag.dto.RagSourceType;
import com.forpets.domain.ai.rag.service.AiRagService;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterProfileStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @InjectMocks
    private AiChatService aiChatService;

    @Mock
    private AiChatClient aiChatClient;

    @Mock
    private AiSitterRecommendationTool sitterRecommendationTool;

    @Mock
    private AiRagService aiRagService;

    @Mock
    private AiChatSessionStore aiChatSessionStore;

    @Test
    @DisplayName("[성공] 사용자 메시지를 자동 Tool Calling 클라이언트에 위임한다")
    void chat_success() {
        // given
        String message = "마포구에서 분리불안 있는 말티즈 맡길 시터 찾아줘";
        String sessionId = "session-1";
        List<RecommendedSitterDto> candidates = List.of(candidate());
        given(aiChatSessionStore.resolveSessionId(null)).willReturn(sessionId);
        given(aiChatSessionStore.loadRecentMessages(1L, sessionId)).willReturn(List.of());
        given(aiChatClient.chatWithTools(message, sitterRecommendationTool))
                .willReturn(new AiChatResponse("마포구에서 소형견 케어가 가능한 시터를 찾았어요.", candidates));
        given(aiRagService.searchSources(message)).willReturn(List.of(source()));

        // when
        AiChatResponse response = aiChatService.chat(1L, new AiChatRequest(message));

        // then
        assertThat(response.answer()).contains("마포구");
        assertThat(response.answer()).contains("리뷰 근거");
        assertThat(response.sessionId()).isEqualTo(sessionId);
        assertThat(response.recommendedSitters()).hasSize(1);
        assertThat(response.sources()).hasSize(1);
        then(aiChatClient).should().chatWithTools(message, sitterRecommendationTool);
        then(aiChatSessionStore).should().appendTurn(1L, sessionId, message, response.answer());
    }

    @Test
    @DisplayName("[성공] 세션 이력이 있으면 최근 대화 맥락을 함께 전달한다")
    void chat_with_history() {
        // given
        String message = "그중에 가격이 낮은 시터로 다시 알려줘";
        String sessionId = "session-1";
        List<AiChatSessionMessage> history = List.of(
                AiChatSessionMessage.user("분리불안 있는 말티즈를 맡길 시터 찾아줘"),
                AiChatSessionMessage.assistant("시터 #7님을 추천드려요.")
        );
        given(aiChatSessionStore.resolveSessionId(sessionId)).willReturn(sessionId);
        given(aiChatSessionStore.loadRecentMessages(1L, sessionId)).willReturn(history);
        given(aiRagService.searchSources(message)).willReturn(List.of());
        given(aiChatClient.chatWithTools(argThat(prompt ->
                        prompt.contains("[최근 대화]")
                                && prompt.contains("이전 추천 후보 범위 안에서")
                                && prompt.contains("이전 조건을 유지")
                                && prompt.contains("조건과 맞지 않는 시터는 가격이 낮거나 경력이 길어도 추천하지 마세요")
                ), eq(sitterRecommendationTool)))
                .willReturn(new AiChatResponse("이전 추천 후보 중 가격이 낮은 시터를 다시 정리했어요.", List.of(candidate())));

        // when
        AiChatResponse response = aiChatService.chat(1L, new AiChatRequest(message, sessionId));

        // then
        assertThat(response.sessionId()).isEqualTo(sessionId);
        assertThat(response.answer()).contains("가격이 낮은 시터");
        then(aiChatSessionStore).should().appendTurn(1L, sessionId, message, response.answer());
    }

    private RagSearchResultDto source() {
        return new RagSearchResultDto(
                RagSourceType.REVIEW,
                10L,
                10L,
                1L,
                5,
                "분리불안 반려견을 차분하게 케어해주셨어요.",
                0.88
        );
    }

    private RecommendedSitterDto candidate() {
        return new RecommendedSitterDto(
                1L,
                3L,
                Region.MAPO,
                "소형견 케어 경험이 많습니다.",
                3,
                PossiblePetType.DOG,
                PossiblePetSize.SMALL,
                15000,
                SitterProfileStatus.RESERVABLE,
                "분리불안 반려견을 차분하게 케어했다는 후기가 많습니다.",
                List.of("분리불안 반려견 대응"),
                List.of(),
                List.of()
        );
    }
}
