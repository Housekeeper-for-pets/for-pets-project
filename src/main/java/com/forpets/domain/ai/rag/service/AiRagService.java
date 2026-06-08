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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AiRagService {

    private final ReviewRepository reviewRepository;
    private final SitterProfileRepository sitterProfileRepository;
    private final RagEmbeddingClient embeddingClient;
    private final RagVectorStore vectorStore;

    @Value("${forpets.ai.rag.search-limit:5}")
    private int searchLimit;

    @Value("${forpets.ai.rag.score-threshold:0.7}")
    private double scoreThreshold;

    public RagIndexResponse indexCompletedReviews() {
        List<Review> reviews = reviewRepository.findAllActiveByReservationStatus(ReservationStatus.COMPLETED);
        List<RagDocument> documents = reviews.stream()
                .map(this::toDocument)
                .flatMap(Optional::stream)
                .toList();

        if (documents.isEmpty()) {
            return new RagIndexResponse(0);
        }

        try {
            vectorStore.initializeCollection();
            List<List<Double>> vectors = new ArrayList<>();
            for (RagDocument document : documents) {
                vectors.add(embeddingClient.embed(document.content()));
            }
            vectorStore.upsertReviews(documents, vectors);
            return new RagIndexResponse(documents.size());
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
                        review.getId(),
                        sitterId,
                        review.getRating(),
                        review.getReviewComment(),
                        RagSourceType.REVIEW
                ));
    }
}
