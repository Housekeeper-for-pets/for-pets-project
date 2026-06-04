package com.forpets.domain.ai.reviewsummary.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.ai.reviewsummary.dto.SitterReviewSummaryResponse;
import com.forpets.domain.ai.reviewsummary.exception.AiReviewSummaryErrorCode;
import com.forpets.domain.ai.reviewsummary.exception.AiReviewSummaryException;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class GeminiReviewSummaryClient implements AiReviewSummaryClient {

    private static final String GEMINI_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    private static final String SYSTEM_INSTRUCTION = """
            당신은 ForPets의 AI 리뷰 요약기입니다.
            반드시 JSON 객체만 반환하고, 리뷰에 없는 사실은 생성하지 마세요.
            의료 행위, 자격증, 전문성을 근거 없이 언급하지 마세요.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final Integer maxTokens;
    private final Double temperature;

    public GeminiReviewSummaryClient(
            ObjectMapper objectMapper,
            @Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${forpets.ai.gemini.model:gemini-2.5-flash-lite}") String model,
            @Value("${forpets.ai.gemini.max-tokens:900}") Integer maxTokens,
            @Value("${forpets.ai.gemini.temperature:0.2}") Double temperature
    ) {
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
            throw new AiReviewSummaryException(AiReviewSummaryErrorCode.AI_REVIEW_SUMMARY_FAILED, "GEMINI_API_KEY가 설정되어 있지 않습니다.");
        }

        try {
            String rawResponse = restClient.post()
                    .uri(GEMINI_URL_TEMPLATE.formatted(model, apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequest(prompt))
                    .retrieve()
                    .body(String.class);

            JsonNode responseNode = objectMapper.readTree(rawResponse);
            String content = extractText(responseNode);
            if (!StringUtils.hasText(content)) {
                throw new AiReviewSummaryException(AiReviewSummaryErrorCode.INVALID_AI_RESPONSE);
            }

            SitterReviewSummaryResponse response =
                    objectMapper.readValue(stripMarkdownFence(content), SitterReviewSummaryResponse.class);
            return new AiReviewSummaryResult(
                    response,
                    model,
                    usageToken(responseNode, "promptTokenCount"),
                    usageToken(responseNode, "candidatesTokenCount"),
                    usageToken(responseNode, "totalTokenCount")
            );
        } catch (AiReviewSummaryException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Gemini 리뷰 요약 응답 처리 실패. model={}, message={}", model, exception.getMessage(), exception);
            throw new AiReviewSummaryException(AiReviewSummaryErrorCode.AI_REVIEW_SUMMARY_FAILED);
        }
    }

    private Map<String, Object> buildRequest(String prompt) {
        // Gemini JSON schema를 함께 넘겨 DTO 필드명과 타입을 강제한다.
        return Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))
                ),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", maxTokens,
                        "responseMimeType", "application/json",
                        "responseSchema", buildResponseSchema()
                )
        );
    }

    private Map<String, Object> buildResponseSchema() {
        return Map.of(
                "type", "object",
                "required", List.of(
                        "summary",
                        "strengths",
                        "cautions",
                        "recommendedFor",
                        "keywords",
                        "sentiment",
                        "confidenceScore",
                        "reviewCount",
                        "usedReviewIds"
                ),
                "properties", Map.of(
                        "summary", Map.of("type", "string"),
                        "strengths", stringArraySchema(),
                        "cautions", stringArraySchema(),
                        "recommendedFor", stringArraySchema(),
                        "keywords", stringArraySchema(),
                        "sentiment", Map.of("type", "string", "enum", List.of("POSITIVE", "NEUTRAL", "NEGATIVE")),
                        "confidenceScore", Map.of("type", "number"),
                        "reviewCount", Map.of("type", "integer"),
                        "usedReviewIds", Map.of(
                                "type", "array",
                                "items", Map.of("type", "integer")
                        )
                )
        );
    }

    private Map<String, Object> stringArraySchema() {
        return Map.of(
                "type", "array",
                "items", Map.of("type", "string")
        );
    }

    private String extractText(JsonNode responseNode) {
        return responseNode.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();
    }

    private Integer usageToken(JsonNode responseNode, String fieldName) {
        JsonNode tokenNode = responseNode.path("usageMetadata").path(fieldName);
        return tokenNode.isNumber() ? tokenNode.asInt() : null;
    }

    private String stripMarkdownFence(String content) {
        return content
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }
}
