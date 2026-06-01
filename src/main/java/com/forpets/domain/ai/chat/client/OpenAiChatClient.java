package com.forpets.domain.ai.chat.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.ai.chat.dto.AiSitterSearchCondition;
import com.forpets.domain.ai.chat.dto.RecommendedSitterDto;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@Profile("ai")
public class OpenAiChatClient implements AiChatClient {

    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final String CONDITION_SYSTEM_MESSAGE = """
            당신은 ForPets의 시터 추천 조건 추출기입니다.
            사용자 문장에서 지역, 반려동물 타입, 크기, 가격, 케어 고민만 JSON으로 추출하세요.
            모르면 null을 사용하고, enum은 서버가 제공한 값만 사용하세요.
            """;
    private static final String ANSWER_SYSTEM_MESSAGE = """
            당신은 ForPets의 시터 추천 도우미입니다.
            제공된 후보 시터 데이터 안에서만 추천하고, DB에 없는 사실이나 의료 행위 가능 여부는 만들지 마세요.
            답변은 한국어로 간결하게 작성하세요.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final Integer maxTokens;
    private final Double temperature;

    public OpenAiChatClient(
            ObjectMapper objectMapper,
            @Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${forpets.ai.openai.model:gpt-4o-mini}") String model,
            @Value("${forpets.ai.chat.max-tokens:700}") Integer maxTokens,
            @Value("${forpets.ai.chat.temperature:0.3}") Double temperature
    ) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    @Override
    public AiSitterSearchCondition extractCondition(String message) {
        if (!StringUtils.hasText(apiKey)) {
            return AiSitterSearchCondition.empty();
        }

        try {
            String content = requestContent(buildConditionRequest(message));
            ConditionResponse response = objectMapper.readValue(content, ConditionResponse.class);
            return response.toCondition();
        } catch (Exception exception) {
            return AiSitterSearchCondition.empty();
        }
    }

    @Override
    public String generateAnswer(String message, AiSitterSearchCondition condition, List<RecommendedSitterDto> candidates) {
        if (candidates.isEmpty()) {
            return "조건에 맞는 시터를 찾지 못했어요. 지역, 반려동물 크기, 가격 조건을 조금 넓혀서 다시 요청해보세요.";
        }
        if (!StringUtils.hasText(apiKey)) {
            return "조건에 맞는 시터 " + candidates.size() + "명을 찾았어요. 시터 상세에서 리뷰 요약과 가능 시간을 함께 확인해보세요.";
        }

        try {
            return requestContent(buildAnswerRequest(message, condition, candidates));
        } catch (Exception exception) {
            return "조건에 맞는 시터 " + candidates.size() + "명을 찾았어요. 추천 후보 목록에서 리뷰 요약과 가능 시간을 확인해보세요.";
        }
    }

    private String requestContent(Map<String, Object> requestBody) {
        String rawResponse = restClient.post()
                .uri(CHAT_COMPLETIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readTree(rawResponse)
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();
        } catch (Exception exception) {
            return "";
        }
    }

    private Map<String, Object> buildConditionRequest(String message) {
        return Map.of(
                "model", model,
                "temperature", 0.1,
                "max_tokens", 300,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", CONDITION_SYSTEM_MESSAGE),
                        Map.of("role", "user", "content", buildConditionPrompt(message))
                )
        );
    }

    private String buildConditionPrompt(String message) {
        return """
                사용자 요청:
                %s

                출력 JSON:
                {
                  "region": "MAPO | GANGNAM | SEOCHO | ... | null",
                  "possiblePetType": "DOG | CAT | ETC | ALL | null",
                  "possiblePetSize": "SMALL | MEDIUM | LARGE | ALL | null",
                  "minPrice": number | null,
                  "maxPrice": number | null,
                  "concern": "string | null"
                }
                """.formatted(message);
    }

    private Map<String, Object> buildAnswerRequest(
            String message,
            AiSitterSearchCondition condition,
            List<RecommendedSitterDto> candidates
    ) {
        return Map.of(
                "model", model,
                "temperature", temperature,
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "system", "content", ANSWER_SYSTEM_MESSAGE),
                        Map.of("role", "user", "content", buildAnswerPrompt(message, condition, candidates))
                )
        );
    }

    private String buildAnswerPrompt(
            String message,
            AiSitterSearchCondition condition,
            List<RecommendedSitterDto> candidates
    ) {
        try {
            return """
                    사용자 요청:
                    %s

                    추출 조건:
                    %s

                    실제 DB 조회 후보:
                    %s

                    위 후보 안에서만 1~3명을 추천하고, 추천 이유를 리뷰 요약과 프로필 기준으로 설명하세요.
                    """.formatted(
                    message,
                    objectMapper.writeValueAsString(condition),
                    objectMapper.writeValueAsString(candidates)
            );
        } catch (Exception exception) {
            return message;
        }
    }

    private record ConditionResponse(
            Region region,
            PossiblePetType possiblePetType,
            PossiblePetSize possiblePetSize,
            Integer minPrice,
            Integer maxPrice,
            String concern
    ) {
        private AiSitterSearchCondition toCondition() {
            return new AiSitterSearchCondition(region, possiblePetType, possiblePetSize, minPrice, maxPrice, concern);
        }
    }
}
