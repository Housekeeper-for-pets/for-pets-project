package com.forpets.domain.ai.chat.service;

import com.forpets.domain.ai.chat.client.AiChatClient;
import com.forpets.domain.ai.chat.dto.AiChatRequest;
import com.forpets.domain.ai.chat.dto.AiChatResponse;
import com.forpets.domain.ai.chat.dto.RecommendedSitterDto;
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

    @Test
    @DisplayName("[성공] 사용자 메시지를 자동 Tool Calling 클라이언트에 위임한다")
    void chat_success() {
        // given
        String message = "마포구에서 분리불안 있는 말티즈 맡길 시터 찾아줘";
        List<RecommendedSitterDto> candidates = List.of(candidate());
        given(aiChatClient.chatWithTools(message, sitterRecommendationTool))
                .willReturn(new AiChatResponse("마포구에서 소형견 케어가 가능한 시터를 찾았어요.", candidates));

        // when
        AiChatResponse response = aiChatService.chat(1L, new AiChatRequest(message));

        // then
        assertThat(response.answer()).contains("마포구");
        assertThat(response.recommendedSitters()).hasSize(1);
        then(aiChatClient).should().chatWithTools(message, sitterRecommendationTool);
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
