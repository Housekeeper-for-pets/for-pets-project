package com.forpets.domain.ai.reviewsummary.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.ai.reviewsummary.dto.SitterReviewSummaryResponse;
import com.forpets.domain.ai.reviewsummary.exception.AiReviewSummaryErrorCode;
import com.forpets.domain.ai.reviewsummary.exception.AiReviewSummaryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@Profile("openai")
public class OpenAiReviewSummaryClient implements AiReviewSummaryClient {

    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final String SYSTEM_MESSAGE = """
            당신은 ForPets의 AI 리뷰 요약기입니다.
            반드시 요청한 JSON 스키마만 반환하고, 리뷰에 없는 사실은 생성하지 마세요.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final Integer maxTokens;
    private final Double temperature;

    public OpenAiReviewSummaryClient(
            ObjectMapper objectMapper,
            @Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${forpets.ai.openai.model:gpt-4o-mini}") String model,
            @Value("${forpets.ai.openai.max-tokens:900}") Integer maxTokens,
            @Value("${forpets.ai.openai.temperature:0.2}") Double temperature) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    @Override
    public AiReviewSummaryResult generate(String prompt) {
        if (!StringUtils.hasText(apiKey)) {
            throw new AiReviewSummaryException(AiReviewSummaryErrorCode.AI_REVIEW_SUMMARY_FAILED, "OPENAI_API_KEY가 설정되어 있지 않습니다.");
        }

        try {
            // 프론트 응답값을 신뢰하지 않고, 서버에서 만든 프롬프트만 OpenAI에 전달한다.
            String rawResponse = restClient.post()
                    .uri(CHAT_COMPLETIONS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(buildRequest(prompt))
                    .retrieve()
                    .body(String.class);

            String content = objectMapper.readTree(rawResponse)
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

            if (!StringUtils.hasText(content)) {
                throw new AiReviewSummaryException(AiReviewSummaryErrorCode.INVALID_AI_RESPONSE);
            }

            SitterReviewSummaryResponse response =
                    objectMapper.readValue(content, SitterReviewSummaryResponse.class);
            return new AiReviewSummaryResult(response, model);
        } catch (AiReviewSummaryException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiReviewSummaryException(AiReviewSummaryErrorCode.AI_REVIEW_SUMMARY_FAILED);
        }
    }

    private Map<String, Object> buildRequest(String prompt) {
        // JSON mode를 사용해 구조화 응답 실패 가능성을 낮춘다.
        return Map.of(
                "model", model,
                "temperature", temperature,
                "max_tokens", maxTokens,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_MESSAGE),
                        Map.of("role", "user", "content", prompt)
                )
        );
    }
}
