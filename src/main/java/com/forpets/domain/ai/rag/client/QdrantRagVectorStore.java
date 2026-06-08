package com.forpets.domain.ai.rag.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.ai.rag.dto.RagDocument;
import com.forpets.domain.ai.rag.dto.RagSearchResultDto;
import com.forpets.domain.ai.rag.dto.RagSourceType;
import com.forpets.domain.ai.rag.exception.AiRagErrorCode;
import com.forpets.domain.ai.rag.exception.AiRagException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class QdrantRagVectorStore implements RagVectorStore {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String collectionName;
    private final Integer vectorSize;

    public QdrantRagVectorStore(
            ObjectMapper objectMapper,
            @Value("${forpets.ai.qdrant.base-url:http://localhost:6333}") String baseUrl,
            @Value("${forpets.ai.rag.collection-name:forpets_reviews}") String collectionName,
            @Value("${forpets.ai.rag.vector-size:768}") Integer vectorSize,
            @Value("${forpets.ai.gemini.connect-timeout-ms:2000}") Long connectTimeoutMs,
            @Value("${forpets.ai.gemini.read-timeout-ms:5000}") Long readTimeoutMs
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(connectTimeoutMs, readTimeoutMs))
                .build();
        this.objectMapper = objectMapper;
        this.collectionName = collectionName;
        this.vectorSize = vectorSize;
    }

    private SimpleClientHttpRequestFactory requestFactory(Long connectTimeoutMs, Long readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return requestFactory;
    }

    @Override
    public void initializeCollection() {
        try {
            restClient.put()
                    .uri("/collections/{collectionName}", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "vectors", Map.of(
                                    "size", vectorSize,
                                    "distance", "Cosine"
                            )
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            log.warn("Qdrant collection 초기화 실패. collection={}, message={}", collectionName, exception.getMessage(), exception);
            throw new AiRagException(AiRagErrorCode.RAG_INDEX_FAILED);
        }
    }

    @Override
    public void upsertReviews(List<RagDocument> documents, List<List<Double>> vectors) {
        if (documents.size() != vectors.size()) {
            throw new AiRagException(AiRagErrorCode.RAG_INDEX_FAILED, "문서 수와 벡터 수가 일치하지 않습니다.");
        }

        try {
            List<Map<String, Object>> points = new ArrayList<>();
            for (int index = 0; index < documents.size(); index++) {
                RagDocument document = documents.get(index);
                points.add(Map.of(
                        "id", document.reviewId(),
                        "vector", vectors.get(index),
                        "payload", payload(document)
                ));
            }

            restClient.put()
                    .uri("/collections/{collectionName}/points?wait=true", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("points", points))
                    .retrieve()
                    .toBodilessEntity();
        } catch (AiRagException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Qdrant review upsert 실패. collection={}, message={}", collectionName, exception.getMessage(), exception);
            throw new AiRagException(AiRagErrorCode.RAG_INDEX_FAILED);
        }
    }

    @Override
    public List<RagSearchResultDto> search(List<Double> queryVector, int limit, double scoreThreshold) {
        try {
            String rawResponse = restClient.post()
                    .uri("/collections/{collectionName}/points/search", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "vector", queryVector,
                            "limit", limit,
                            "score_threshold", scoreThreshold,
                            "with_payload", true
                    ))
                    .retrieve()
                    .body(String.class);

            JsonNode results = objectMapper.readTree(rawResponse).path("result");
            if (!results.isArray()) {
                return List.of();
            }

            List<RagSearchResultDto> searchResults = new ArrayList<>();
            results.forEach(result -> searchResults.add(toSearchResult(result)));
            return searchResults;
        } catch (Exception exception) {
            log.warn("Qdrant search 실패. collection={}, message={}", collectionName, exception.getMessage(), exception);
            throw new AiRagException(AiRagErrorCode.RAG_SEARCH_FAILED);
        }
    }

    private Map<String, Object> payload(RagDocument document) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceType", document.sourceType().name());
        payload.put("reviewId", document.reviewId());
        payload.put("sitterId", document.sitterId());
        payload.put("rating", document.rating());
        payload.put("content", document.content());
        return payload;
    }

    private RagSearchResultDto toSearchResult(JsonNode result) {
        JsonNode payload = result.path("payload");
        return new RagSearchResultDto(
                RagSourceType.valueOf(payload.path("sourceType").asText(RagSourceType.REVIEW.name())),
                payload.path("reviewId").asLong(),
                payload.path("sitterId").asLong(),
                payload.path("rating").asInt(),
                payload.path("content").asText(),
                result.path("score").asDouble()
        );
    }
}
