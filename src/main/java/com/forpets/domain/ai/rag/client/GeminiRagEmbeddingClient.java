package com.forpets.domain.ai.rag.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.ai.rag.exception.AiRagErrorCode;
import com.forpets.domain.ai.rag.exception.AiRagException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Profile("ai")
@Slf4j
public class GeminiRagEmbeddingClient implements RagEmbeddingClient {

    private static final String GEMINI_EMBEDDING_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:embedContent?key=%s";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String embeddingModel;

    public GeminiRagEmbeddingClient(
            ObjectMapper objectMapper,
            @Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${forpets.ai.gemini.embedding-model:text-embedding-004}") String embeddingModel,
            @Value("${forpets.ai.gemini.connect-timeout-ms:2000}") Long connectTimeoutMs,
            @Value("${forpets.ai.gemini.read-timeout-ms:5000}") Long readTimeoutMs
    ) {
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory(connectTimeoutMs, readTimeoutMs))
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.embeddingModel = embeddingModel;
    }

    private SimpleClientHttpRequestFactory requestFactory(Long connectTimeoutMs, Long readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return requestFactory;
    }

    @Override
    public List<Double> embed(String text) {
        if (!StringUtils.hasText(apiKey)) {
            throw new AiRagException(AiRagErrorCode.RAG_EMBEDDING_FAILED, "GEMINI_API_KEY가 설정되어 있지 않습니다.");
        }

        try {
            String rawResponse = restClient.post()
                    .uri(GEMINI_EMBEDDING_URL_TEMPLATE.formatted(embeddingModel, apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequest(text))
                    .retrieve()
                    .body(String.class);

            JsonNode values = objectMapper.readTree(rawResponse)
                    .path("embedding")
                    .path("values");
            if (!values.isArray()) {
                throw new AiRagException(AiRagErrorCode.RAG_EMBEDDING_FAILED);
            }

            List<Double> embedding = new ArrayList<>();
            values.forEach(value -> embedding.add(value.asDouble()));
            return embedding;
        } catch (AiRagException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Gemini 임베딩 생성 실패. model={}, message={}", embeddingModel, exception.getMessage(), exception);
            throw new AiRagException(AiRagErrorCode.RAG_EMBEDDING_FAILED);
        }
    }

    private Map<String, Object> buildRequest(String text) {
        return Map.of(
                "model", "models/" + embeddingModel,
                "content", Map.of(
                        "parts", List.of(Map.of("text", text))
                )
        );
    }
}
