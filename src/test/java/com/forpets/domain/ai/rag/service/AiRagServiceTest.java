package com.forpets.domain.ai.rag.service;

import com.forpets.domain.ai.rag.client.RagEmbeddingClient;
import com.forpets.domain.ai.rag.client.RagVectorStore;
import com.forpets.domain.ai.rag.dto.RagIndexResponse;
import com.forpets.domain.ai.rag.dto.RagSearchResultDto;
import com.forpets.domain.ai.rag.dto.RagSourceType;
import com.forpets.domain.ai.reviewsummary.entity.ReviewSentiment;
import com.forpets.domain.ai.reviewsummary.entity.SitterReviewSummary;
import com.forpets.domain.ai.reviewsummary.entity.SummaryStatus;
import com.forpets.domain.ai.reviewsummary.repository.SitterReviewSummaryRepository;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.review.entity.Review;
import com.forpets.domain.review.repository.ReviewRepository;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AiRagServiceTest {

    @InjectMocks
    private AiRagService aiRagService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private SitterProfileRepository sitterProfileRepository;

    @Mock
    private SitterReviewSummaryRepository summaryRepository;

    @Mock
    private RagEmbeddingClient embeddingClient;

    @Mock
    private RagVectorStore vectorStore;

    @Test
    @DisplayName("[성공] 완료 예약의 리뷰를 임베딩하여 Qdrant에 저장한다")
    void index_completed_reviews() {
        // given
        Review review = review(10L, 3L, "분리불안 반려견을 차분하게 케어해주셨어요.");
        SitterProfile sitterProfile = mock(SitterProfile.class);
        given(sitterProfile.getId()).willReturn(1L);
        given(reviewRepository.findAllActiveByReservationStatus(ReservationStatus.COMPLETED)).willReturn(List.of(review));
        given(sitterProfileRepository.findByMemberId(3L)).willReturn(Optional.of(sitterProfile));
        given(embeddingClient.embed(review.getReviewComment())).willReturn(List.of(0.1, 0.2, 0.3));

        // when
        RagIndexResponse response = aiRagService.indexCompletedReviews();

        // then
        assertThat(response.indexedCount()).isEqualTo(1);
        ArgumentCaptor<List> documentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> vectorCaptor = ArgumentCaptor.forClass(List.class);
        then(vectorStore).should().initializeCollection();
        then(vectorStore).should().upsertReviews(documentCaptor.capture(), vectorCaptor.capture());
        assertThat(documentCaptor.getValue()).hasSize(1);
        assertThat(vectorCaptor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("[성공] 저장된 AI 리뷰 요약도 RAG 인덱싱 대상에 포함한다")
    void index_review_summaries() {
        // given
        SitterReviewSummary summary = summary(20L, 1L);
        given(reviewRepository.findAllActiveByReservationStatus(ReservationStatus.COMPLETED)).willReturn(List.of());
        given(summaryRepository.findAll()).willReturn(List.of(summary));
        given(embeddingClient.embed(org.mockito.ArgumentMatchers.contains("분리불안"))).willReturn(List.of(0.1, 0.2, 0.3));

        // when
        RagIndexResponse response = aiRagService.indexCompletedReviews();

        // then
        assertThat(response.indexedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isZero();
        ArgumentCaptor<List> documentCaptor = ArgumentCaptor.forClass(List.class);
        then(vectorStore).should().upsertReviews(documentCaptor.capture(), org.mockito.ArgumentMatchers.anyList());
        assertThat(documentCaptor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("[성공] 일부 문서 임베딩 실패 시 성공 문서는 저장하고 실패 건수를 반환한다")
    void index_partial_failure() {
        // given
        Review successReview = review(10L, 3L, "분리불안 반려견을 차분하게 케어해주셨어요.");
        Review failedReview = review(11L, 3L, "낯가림 있는 아이 케어 후기입니다.");
        SitterProfile sitterProfile = mock(SitterProfile.class);
        given(sitterProfile.getId()).willReturn(1L);
        given(reviewRepository.findAllActiveByReservationStatus(ReservationStatus.COMPLETED)).willReturn(List.of(successReview, failedReview));
        given(sitterProfileRepository.findByMemberId(3L)).willReturn(Optional.of(sitterProfile));
        given(embeddingClient.embed(successReview.getReviewComment())).willReturn(List.of(0.1, 0.2, 0.3));
        willThrow(new RuntimeException("embedding failed")).given(embeddingClient).embed(failedReview.getReviewComment());

        // when
        RagIndexResponse response = aiRagService.indexCompletedReviews();

        // then
        assertThat(response.indexedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isEqualTo(1);
        ArgumentCaptor<List> documentCaptor = ArgumentCaptor.forClass(List.class);
        then(vectorStore).should().upsertReviews(documentCaptor.capture(), org.mockito.ArgumentMatchers.anyList());
        assertThat(documentCaptor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("[성공] 사용자 질문으로 유사 리뷰 출처를 검색한다")
    void search_sources() {
        // given
        String query = "분리불안 있는 말티즈";
        List<Double> vector = List.of(0.1, 0.2, 0.3);
        given(embeddingClient.embed(query)).willReturn(vector);
        given(vectorStore.search(anyListOfDouble(), anyInt(), anyDouble())).willReturn(List.of(source()));

        // when
        List<RagSearchResultDto> result = aiRagService.searchSources(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).reviewId()).isEqualTo(10L);
        assertThat(result.get(0).snippet()).contains("분리불안");
    }

    @Test
    @DisplayName("[성공] Qdrant 검색 실패는 빈 결과로 fallback 한다")
    void search_sources_fallback_when_vector_store_failed() {
        // given
        String query = "분리불안 있는 말티즈";
        given(embeddingClient.embed(query)).willReturn(List.of(0.1, 0.2, 0.3));
        doThrow(new RuntimeException("qdrant down")).when(vectorStore).search(anyListOfDouble(), anyInt(), anyDouble());

        // when
        List<RagSearchResultDto> result = aiRagService.searchSources(query);

        // then
        assertThat(result).isEmpty();
    }

    private Review review(Long reviewId, Long revieweeId, String comment) {
        Review review = Review.builder()
                .reservationId(1L)
                .reviewerId(2L)
                .revieweeId(revieweeId)
                .reviewComment(comment)
                .rating(5)
                .build();
        ReflectionTestUtils.setField(review, "id", reviewId);
        return review;
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

    private SitterReviewSummary summary(Long summaryId, Long sitterId) {
        SitterReviewSummary summary = SitterReviewSummary.builder()
                .sitterId(sitterId)
                .summary("분리불안 반려견을 차분하게 돌봤다는 후기가 많습니다.")
                .strengths("[\"분리불안 케어\"]")
                .cautions("[]")
                .recommendedFor("[\"소형견\"]")
                .keywords("[\"분리불안\", \"소형견\"]")
                .sentiment(ReviewSentiment.POSITIVE)
                .confidenceScore(0.9)
                .reviewCount(3)
                .aiGenerated(true)
                .model("gemini")
                .promptVersion("review-summary-v1")
                .summaryStatus(SummaryStatus.FRESH)
                .build();
        ReflectionTestUtils.setField(summary, "id", summaryId);
        return summary;
    }

    private List<Double> anyListOfDouble() {
        return org.mockito.ArgumentMatchers.anyList();
    }
}
