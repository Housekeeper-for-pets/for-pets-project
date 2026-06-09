package com.forpets.domain.ai.rag.service;

import com.forpets.domain.ai.rag.client.RagEmbeddingClient;
import com.forpets.domain.ai.rag.client.RagVectorStore;
import com.forpets.domain.ai.rag.dto.RagDocument;
import com.forpets.domain.ai.rag.dto.RagIndexResponse;
import com.forpets.domain.ai.rag.dto.RagSearchResponse;
import com.forpets.domain.ai.rag.dto.RagSearchResultDto;
import com.forpets.domain.ai.rag.dto.RagSourceType;
import com.forpets.domain.ai.rag.exception.AiRagErrorCode;
import com.forpets.domain.ai.rag.exception.AiRagException;
import com.forpets.domain.ai.reviewsummary.entity.SitterReviewSummary;
import com.forpets.domain.ai.reviewsummary.entity.SummaryStatus;
import com.forpets.domain.ai.reviewsummary.repository.SitterReviewSummaryRepository;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.review.entity.Review;
import com.forpets.domain.review.repository.ReviewRepository;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AiRagService {

    private final ReviewRepository reviewRepository;
    private final SitterProfileRepository sitterProfileRepository;
    private final SitterReviewSummaryRepository summaryRepository;
    private final RagEmbeddingClient embeddingClient;
    private final RagVectorStore vectorStore;

    @Value("${forpets.ai.rag.search-limit:5}")
    private int searchLimit;

    @Value("${forpets.ai.rag.score-threshold:0.7}")
    private double scoreThreshold;

    public RagIndexResponse indexCompletedReviews() {
        List<Review> reviews = reviewRepository.findAllActiveByReservationStatus(ReservationStatus.COMPLETED);
        List<RagDocument> reviewDocuments = reviews.stream()
                .map(this::toDocument)
                .flatMap(Optional::stream)
                .toList();
        List<RagDocument> summaryDocuments = summaryRepository.findAll()
                .stream()
                .filter(summary -> summary.getSummaryStatus() != SummaryStatus.FAILED)
                .map(this::toSummaryDocument)
                .toList();
        List<RagDocument> documents = new ArrayList<>();
        documents.addAll(reviewDocuments);
        documents.addAll(summaryDocuments);

        if (documents.isEmpty()) {
            return new RagIndexResponse(0, 0);
        }

        try {
            vectorStore.initializeCollection();
            List<RagDocument> indexedDocuments = new ArrayList<>();
            List<List<Double>> vectors = new ArrayList<>();
            for (RagDocument document : documents) {
                try {
                    vectors.add(embeddingClient.embed(document.content()));
                    indexedDocuments.add(document);
                } catch (Exception exception) {
                    log.warn("RAG 문서 임베딩 실패. sourceType={}, sourceId={}, message={}",
                            document.sourceType(), document.sourceId(), exception.getMessage(), exception);
                }
            }

            if (!indexedDocuments.isEmpty()) {
                vectorStore.upsertReviews(indexedDocuments, vectors);
            }
            return new RagIndexResponse(indexedDocuments.size(), documents.size() - indexedDocuments.size());
        } catch (AiRagException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("RAG 리뷰 인덱싱 실패. message={}", exception.getMessage(), exception);
            throw new AiRagException(AiRagErrorCode.RAG_INDEX_FAILED);
        }
    }

    public RagSearchResponse search(String query) {
        return new RagSearchResponse(searchSources(query));
    }

    public List<RagSearchResultDto> searchSources(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        try {
            List<Double> queryVector = embeddingClient.embed(query);
            return vectorStore.search(queryVector, searchLimit, scoreThreshold);
        } catch (Exception exception) {
            // RAG는 추천 보조 기능이므로 검색 실패가 챗봇 전체 실패로 전파되지 않게 한다.
            log.warn("RAG 검색 실패. Tool Calling 추천만 사용합니다. message={}", exception.getMessage(), exception);
            return List.of();
        }
    }

    private Optional<RagDocument> toDocument(Review review) {
        return sitterProfileRepository.findByMemberId(review.getRevieweeId())
                .map(SitterProfile::getId)
                .map(sitterId -> new RagDocument(
                        pointId(RagSourceType.REVIEW, review.getId()),
                        review.getId(),
                        sitterId,
                        review.getRating(),
                        review.getReviewComment(),
                        RagSourceType.REVIEW
                ));
    }

    private RagDocument toSummaryDocument(SitterReviewSummary summary) {
        return new RagDocument(
                pointId(RagSourceType.REVIEW_SUMMARY, summary.getId()),
                summary.getId(),
                summary.getSitterId(),
                null,
                summaryContent(summary),
                RagSourceType.REVIEW_SUMMARY
        );
    }

    private String summaryContent(SitterReviewSummary summary) {
        return """
                요약: %s
                강점: %s
                주의점: %s
                추천 대상: %s
                키워드: %s
                """.formatted(
                summary.getSummary(),
                summary.getStrengths(),
                summary.getCautions(),
                summary.getRecommendedFor(),
                summary.getKeywords()
        );
    }

    private String pointId(RagSourceType sourceType, Long sourceId) {
        String rawId = sourceType.name() + ":" + sourceId;
        return UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
