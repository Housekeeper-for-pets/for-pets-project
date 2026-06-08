package com.forpets.domain.ai.chat.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.forpets.domain.ai.chat.dto.AiChatResponse;
import com.forpets.domain.ai.chat.dto.AiSitterSearchCondition;
import com.forpets.domain.ai.chat.dto.RecommendedSitterDto;
import com.forpets.domain.ai.chat.service.AiSitterRecommendationTool;
import com.forpets.domain.ai.usage.entity.AiErrorType;
import com.forpets.domain.ai.usage.entity.AiFeature;
import com.forpets.domain.ai.usage.service.AiUsageLogService;
import com.forpets.domain.ai.usage.service.AiUsageRecord;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Profile("ai")
@Slf4j
public class GeminiChatClient implements AiChatClient {

    private static final String GEMINI_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    private static final String CONDITION_SYSTEM_INSTRUCTION = """
            당신은 ForPets의 시터 추천 조건 추출기입니다.
            사용자 문장에서 지역, 반려동물 타입, 크기, 가격, 케어 고민만 JSON으로 추출하세요.
            모르면 null을 사용하고 enum은 서버가 제공한 값만 사용하세요.
            """;
    private static final String ANSWER_SYSTEM_INSTRUCTION = """
            당신은 ForPets의 시터 추천 도우미입니다.
            제공된 후보 시터 데이터 안에서만 추천하고, DB에 없는 사실이나 의료 행위 가능 여부는 만들지 마세요.
            답변은 한국어로 간결하게 작성하세요.
            """;
    private static final String TOOL_CALLING_SYSTEM_INSTRUCTION = """
            당신은 ForPets의 시터 추천 도우미입니다.
            사용자의 자연어 요청을 해결하기 위해 제공된 Tool을 직접 호출하세요.
            Tool 결과에 없는 시터, 리뷰, 가능 시간, 의료 행위 가능 여부는 만들지 마세요.
            리뷰 근거가 필요한 요청은 searchRelevantReviewSources Tool을 호출하고, 답변에는 reviewId 또는 sitterId 출처를 간단히 포함하세요.
            추천 후보가 있으면 리뷰 요약과 가능 시간을 근거로 한국어로 간결하게 답변하세요.
            Tool 호출 실패 컨텍스트가 있으면 사용자에게 자연스럽게 안내하세요.
            """;
    private static final int MAX_TOOL_CALL_TURNS = 4;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final Integer maxTokens;
    private final Double temperature;
    private final AiUsageLogService aiUsageLogService;

    public GeminiChatClient(
            ObjectMapper objectMapper,
            @Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${forpets.ai.gemini.model:gemini-2.5-flash-lite}") String model,
            @Value("${forpets.ai.chat.max-tokens:700}") Integer maxTokens,
            @Value("${forpets.ai.chat.temperature:0.3}") Double temperature,
            @Value("${forpets.ai.gemini.connect-timeout-ms:2000}") Long connectTimeoutMs,
            @Value("${forpets.ai.gemini.read-timeout-ms:5000}") Long readTimeoutMs,
            AiUsageLogService aiUsageLogService
    ) {
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory(connectTimeoutMs, readTimeoutMs))
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.aiUsageLogService = aiUsageLogService;
    }

    private SimpleClientHttpRequestFactory requestFactory(Long connectTimeoutMs, Long readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return requestFactory;
    }

    @Override
    public AiSitterSearchCondition extractCondition(String message) {
        if (!StringUtils.hasText(apiKey)) {
            return AiSitterSearchCondition.empty();
        }

        try {
            String content = requestContent(CONDITION_SYSTEM_INSTRUCTION, buildConditionPrompt(message), 0.1, 300, true);
            ConditionResponse response = objectMapper.readValue(stripMarkdownFence(content), ConditionResponse.class);
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
            return requestContent(ANSWER_SYSTEM_INSTRUCTION, buildAnswerPrompt(message, condition, candidates), temperature, maxTokens, false);
        } catch (Exception exception) {
            log.warn("Gemini 추천 답변 생성 실패. 서버 기본 답변으로 대체합니다.", exception);
            return buildLocalAnswer(candidates);
        }
    }

    @Override
    public AiChatResponse chatWithTools(String message, AiSitterRecommendationTool sitterRecommendationTool) {
        if (!StringUtils.hasText(apiKey)) {
            return AiChatClient.super.chatWithTools(message, sitterRecommendationTool);
        }

        long startedAt = System.nanoTime();
        UsageTokens totalUsage = UsageTokens.empty();
        try {
            ToolCallback[] callbacks = ToolCallbacks.from(sitterRecommendationTool);
            Map<String, ToolCallback> callbackByName = Arrays.stream(callbacks)
                    .collect(Collectors.toMap(ToolCallback::getName, Function.identity()));
            List<Map<String, Object>> contents = new ArrayList<>();
            contents.add(userContent(message));
            Optional<AiSitterSearchCondition> lastSearchCondition = Optional.empty();

            for (int turn = 0; turn < MAX_TOOL_CALL_TURNS; turn++) {
                JsonNode response = requestToolCallingContent(contents, callbacks);
                totalUsage = totalUsage.plus(usageTokens(response));
                List<JsonNode> functionCalls = findFunctionCalls(response);

                if (functionCalls.isEmpty()) {
                    String answer = extractText(response);
                    List<RecommendedSitterDto> candidates = lastSearchCondition
                            .map(sitterRecommendationTool::buildRecommendations)
                            .orElseGet(List::of);
                    String fallbackAnswer = candidates.isEmpty()
                            ? "조건에 맞는 시터를 찾지 못했어요. 지역이나 반려동물 조건을 조금 넓혀서 다시 요청해보세요."
                            : buildLocalAnswer(candidates);
                    recordToolCallingSuccess(totalUsage, elapsedMs(startedAt));
                    return new AiChatResponse(StringUtils.hasText(answer) ? answer : fallbackAnswer, candidates);
                }

                contents.add(modelContent(functionCalls));

                for (JsonNode functionCall : functionCalls) {
                    String name = functionCall.path("name").asText();
                    String args = functionCall.path("args").isMissingNode()
                            ? "{}"
                            : objectMapper.writeValueAsString(functionCall.path("args"));
                    ToolCallback callback = callbackByName.get(name);
                    String result = callback == null
                            ? "{\"status\":\"FAILED\",\"message\":\"등록되지 않은 Tool입니다.\"}"
                            : callback.call(args);

                    if ("searchSitters".equals(name)) {
                        lastSearchCondition = parseSearchCondition(args);
                    }

                    contents.add(toolResponseContent(name, result));
                }
            }

            recordToolCallingFallback(startedAt, AiErrorType.UNKNOWN, "MAX_TOOL_CALL_TURNS_EXCEEDED");
            return AiChatClient.super.chatWithTools(message, sitterRecommendationTool);
        } catch (Exception exception) {
            log.warn("Gemini 자동 Tool Calling 실패. 서버 오케스트레이션 fallback으로 대체합니다.", exception);
            recordToolCallingFallback(startedAt, classify(exception), exception.getMessage());
            return AiChatClient.super.chatWithTools(message, sitterRecommendationTool);
        }
    }

    private void recordToolCallingSuccess(UsageTokens usageTokens, long latencyMs) {
        aiUsageLogService.record(AiUsageRecord.success(
                AiFeature.SITTER_RECOMMENDATION,
                model,
                usageTokens.promptTokens(),
                usageTokens.completionTokens(),
                usageTokens.totalTokens(),
                latencyMs,
                null
        ));
    }

    private void recordToolCallingFallback(long startedAt, AiErrorType errorType, String failureReason) {
        aiUsageLogService.record(AiUsageRecord.fallback(
                AiFeature.SITTER_RECOMMENDATION,
                model,
                elapsedMs(startedAt),
                errorType,
                null,
                failureReason
        ));
    }

    private AiErrorType classify(Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            return AiErrorType.UNKNOWN;
        }
        if (message.contains("429") || message.toLowerCase().contains("rate")) {
            return AiErrorType.RATE_LIMIT;
        }
        if (message.contains("500") || message.contains("502") || message.contains("503")) {
            return AiErrorType.SERVER_ERROR;
        }
        if (message.toLowerCase().contains("timed out") || message.toLowerCase().contains("timeout")) {
            return AiErrorType.TIMEOUT;
        }
        return AiErrorType.UNKNOWN;
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String requestContent(String systemInstruction, String prompt, Double temperature, Integer maxTokens, boolean jsonResponse) throws Exception {
        String rawResponse = restClient.post()
                .uri(GEMINI_URL_TEMPLATE.formatted(model, apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildRequest(systemInstruction, prompt, temperature, maxTokens, jsonResponse))
                .retrieve()
                .body(String.class);

        return objectMapper.readTree(rawResponse)
                .path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();
    }

    private JsonNode requestToolCallingContent(List<Map<String, Object>> contents, ToolCallback[] callbacks) throws Exception {
        String rawResponse = restClient.post()
                .uri(GEMINI_URL_TEMPLATE.formatted(model, apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildToolCallingRequest(contents, callbacks))
                .retrieve()
                .body(String.class);

        return objectMapper.readTree(rawResponse);
    }

    private Map<String, Object> buildRequest(
            String systemInstruction,
            String prompt,
            Double temperature,
            Integer maxTokens,
            boolean jsonResponse
    ) {
        Map<String, Object> generationConfig = jsonResponse
                ? Map.of("temperature", temperature, "maxOutputTokens", maxTokens, "responseMimeType", "application/json")
                : Map.of("temperature", temperature, "maxOutputTokens", maxTokens);

        return Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemInstruction))
                ),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", generationConfig
        );
    }

    private Map<String, Object> buildToolCallingRequest(List<Map<String, Object>> contents, ToolCallback[] callbacks) {
        return Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", TOOL_CALLING_SYSTEM_INSTRUCTION))
                ),
                "contents", contents,
                "tools", List.of(Map.of("functionDeclarations", buildFunctionDeclarations(callbacks))),
                "toolConfig", Map.of("functionCallingConfig", Map.of("mode", "AUTO")),
                "generationConfig", Map.of("temperature", temperature, "maxOutputTokens", maxTokens)
        );
    }

    private List<Map<String, Object>> buildFunctionDeclarations(ToolCallback[] callbacks) {
        return Arrays.stream(callbacks)
                .map(callback -> Map.of(
                        "name", callback.getName(),
                        "description", callback.getDescription(),
                        "parameters", toGeminiSchema(callback.getInputTypeSchema())
                ))
                .toList();
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

    private Map<String, Object> userContent(String message) {
        return Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", message))
        );
    }

    private Map<String, Object> modelContent(List<JsonNode> functionCalls) {
        return Map.of(
                "role", "model",
                "parts", functionCalls.stream()
                        .map(functionCall -> Map.of("functionCall", functionCall))
                        .toList()
        );
    }

    private Map<String, Object> toolResponseContent(String name, String result) {
        return Map.of(
                "role", "user",
                "parts", List.of(Map.of("functionResponse", Map.of(
                        "name", name,
                        "response", Map.of("result", parseJsonOrText(result))
                )))
        );
    }

    private List<JsonNode> findFunctionCalls(JsonNode response) {
        List<JsonNode> functionCalls = new ArrayList<>();
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) {
            return functionCalls;
        }

        for (JsonNode part : parts) {
            if (part.has("functionCall")) {
                functionCalls.add(part.path("functionCall"));
            }
        }
        return functionCalls;
    }

    private UsageTokens usageTokens(JsonNode response) {
        JsonNode usage = response.path("usageMetadata");
        return new UsageTokens(
                token(usage, "promptTokenCount"),
                token(usage, "candidatesTokenCount"),
                token(usage, "totalTokenCount")
        );
    }

    private Integer token(JsonNode usage, String fieldName) {
        JsonNode tokenNode = usage.path(fieldName);
        return tokenNode.isNumber() ? tokenNode.asInt() : null;
    }

    private String extractText(JsonNode response) {
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) {
            return "";
        }

        for (JsonNode part : parts) {
            String text = part.path("text").asText();
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return "";
    }

    private Optional<AiSitterSearchCondition> parseSearchCondition(String args) {
        try {
            JsonNode root = objectMapper.readTree(args);
            JsonNode conditionNode = root.has("condition") ? root.path("condition") : root;
            return Optional.of(objectMapper.treeToValue(conditionNode, AiSitterSearchCondition.class));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private Object parseJsonOrText(String result) {
        try {
            return objectMapper.readValue(result, Object.class);
        } catch (Exception exception) {
            return result;
        }
    }

    private Map<String, Object> toGeminiSchema(String schema) {
        try {
            return objectMapper.convertValue(convertSchema(objectMapper.readTree(schema)), new com.fasterxml.jackson.core.type.TypeReference<>() {
            });
        } catch (Exception exception) {
            return Map.of("type", "OBJECT");
        }
    }

    private JsonNode convertSchema(JsonNode schema) {
        if (schema == null || schema.isNull()) {
            return objectMapper.createObjectNode().put("type", "OBJECT");
        }
        if (schema.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            schema.forEach(item -> arrayNode.add(convertSchema(item)));
            return arrayNode;
        }
        if (!schema.isObject()) {
            return schema;
        }

        ObjectNode converted = objectMapper.createObjectNode();
        schema.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode value = entry.getValue();
            if ("$schema".equals(fieldName) || "additionalProperties".equals(fieldName)) {
                return;
            }
            if ("type".equals(fieldName)) {
                converted.put("type", value.asText().toUpperCase());
                return;
            }
            converted.set(fieldName, convertSchema(value));
        });

        if (!converted.has("type")) {
            converted.put("type", "OBJECT");
        }
        return converted;
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
                    objectMapper.writeValueAsString(toCompactCandidates(candidates))
            );
        } catch (Exception exception) {
            return message;
        }
    }

    private List<Map<String, Object>> toCompactCandidates(List<RecommendedSitterDto> candidates) {
        return candidates.stream()
                .map(candidate -> {
                    Map<String, Object> compact = new LinkedHashMap<>();
                    compact.put("sitterId", candidate.sitterId());
                    compact.put("region", candidate.region());
                    compact.put("experienceYears", candidate.experienceYears());
                    compact.put("possiblePetType", candidate.possiblePetType());
                    compact.put("possiblePetSize", candidate.possiblePetSize());
                    compact.put("pricePerHour", candidate.pricePerHour());
                    compact.put("reviewSummary", candidate.reviewSummary() == null ? "" : candidate.reviewSummary());
                    compact.put("strengths", candidate.strengths());
                    compact.put("cautions", candidate.cautions());
                    return compact;
                })
                .toList();
    }

    private String buildLocalAnswer(List<RecommendedSitterDto> candidates) {
        RecommendedSitterDto first = candidates.get(0);
        String summary = StringUtils.hasText(first.reviewSummary())
                ? first.reviewSummary()
                : "리뷰 요약은 아직 없지만, 조건에 맞는 시터 후보입니다.";

        return "조건에 맞는 시터 " + candidates.size() + "명을 찾았어요. "
                + "우선 " + first.region().getDescription() + " 지역의 시터 #" + first.sitterId()
                + "님을 추천드려요. " + summary;
    }

    private String stripMarkdownFence(String content) {
        return content
                .replace("```json", "")
                .replace("```", "")
                .trim();
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

    private record UsageTokens(
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens
    ) {
        private static UsageTokens empty() {
            return new UsageTokens(null, null, null);
        }

        private UsageTokens plus(UsageTokens other) {
            return new UsageTokens(
                    sum(promptTokens, other.promptTokens),
                    sum(completionTokens, other.completionTokens),
                    sum(totalTokens, other.totalTokens)
            );
        }

        private Integer sum(Integer left, Integer right) {
            if (left == null) {
                return right;
            }
            if (right == null) {
                return left;
            }
            return left + right;
        }
    }
}
