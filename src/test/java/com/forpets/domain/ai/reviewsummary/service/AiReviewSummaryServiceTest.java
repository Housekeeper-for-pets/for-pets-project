package com.forpets.domain.ai.reviewsummary.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.ai.reviewsummary.client.AiReviewSummaryClient;
import com.forpets.domain.ai.reviewsummary.dto.ReviewSource;
import com.forpets.domain.ai.reviewsummary.dto.SitterReviewSummaryDto;
import com.forpets.domain.ai.reviewsummary.dto.SitterReviewSummaryResponse;
import com.forpets.domain.ai.reviewsummary.entity.*;
import com.forpets.domain.ai.reviewsummary.exception.AiReviewSummaryErrorCode;
import com.forpets.domain.ai.reviewsummary.exception.AiReviewSummaryException;
import com.forpets.domain.ai.reviewsummary.repository.AiPromptTemplateRepository;
import com.forpets.domain.ai.reviewsummary.repository.SitterReviewSummaryRepository;
import com.forpets.domain.ai.reviewsummary.repository.SitterReviewSummaryReviewRepository;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AiReviewSummaryServiceTest {

    @InjectMocks
    private AiReviewSummaryService aiReviewSummaryService;

    @Mock
    private SitterProfileRepository sitterProfileRepository;

    @Mock
    private SitterReviewSummaryRepository summaryRepository;

    @Mock
    private SitterReviewSummaryReviewRepository summaryReviewRepository;

    @Mock
    private AiPromptTemplateRepository promptTemplateRepository;

    @Mock
    private ReviewSourceProvider reviewSourceProvider;

    @Mock
    private AiReviewSummaryClient aiReviewSummaryClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private final Long sitterId = 1L;

    @Test
    @DisplayName("[성공] 리뷰를 AI 요약으로 변환해 저장한다")
    void generate_review_summary_success() {
        // given
        List<ReviewSource> reviews = reviews();
        AiPromptTemplate promptTemplate = promptTemplate(PromptCategory.SMALL_DOG);
        SitterReviewSummaryResponse response = response(List.of(1001L, 1002L, 1003L));

        given(sitterProfileRepository.existsById(sitterId)).willReturn(true);
        given(reviewSourceProvider.findRecentReviewsBySitterId(sitterId, 20)).willReturn(reviews);
        given(promptTemplateRepository.findFirstByFeatureAndCategoryAndActiveTrueOrderByIdDesc(
                "SITTER_REVIEW_SUMMARY", PromptCategory.SMALL_DOG))
                .willReturn(Optional.of(promptTemplate));
        given(aiReviewSummaryClient.generate(anyString()))
                .willReturn(new AiReviewSummaryClient.AiReviewSummaryResult(response, "stub-review-summary"));
        given(summaryRepository.findBySitterId(sitterId)).willReturn(Optional.empty());
        given(summaryRepository.save(any(SitterReviewSummary.class)))
                .willAnswer(invocation -> {
                    SitterReviewSummary saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 10L);
                    return saved;
                });

        // when
        SitterReviewSummaryDto result = aiReviewSummaryService.generateReviewSummary(sitterId);

        // then
        assertThat(result.sitterId()).isEqualTo(sitterId);
        assertThat(result.sentiment()).isEqualTo("POSITIVE");
        assertThat(result.summaryStatus()).isEqualTo(SummaryStatus.FRESH);
        assertThat(result.strengths()).contains("소형견 케어");
        then(summaryReviewRepository).should().deleteAllBySummaryId(10L);
        then(summaryReviewRepository).should().saveAll(anyList());
    }

    @Test
    @DisplayName("[실패] 요약할 리뷰가 없으면 AI를 호출하지 않는다")
    void generate_review_summary_empty_reviews() {
        // given
        given(sitterProfileRepository.existsById(sitterId)).willReturn(true);
        given(reviewSourceProvider.findRecentReviewsBySitterId(sitterId, 20)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> aiReviewSummaryService.generateReviewSummary(sitterId))
                .isInstanceOf(AiReviewSummaryException.class)
                .satisfies(ex -> assertThat(((AiReviewSummaryException) ex).getErrorCode())
                        .isEqualTo(AiReviewSummaryErrorCode.REVIEW_SOURCE_NOT_FOUND));

        then(aiReviewSummaryClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("[성공] AI가 입력에 없는 reviewId를 반환하면 입력 리뷰 ID로 보정한다")
    void generate_review_summary_normalize_invalid_used_review_ids() {
        // given
        List<ReviewSource> reviews = reviews();
        AiPromptTemplate promptTemplate = promptTemplate(PromptCategory.SMALL_DOG);

        given(sitterProfileRepository.existsById(sitterId)).willReturn(true);
        given(reviewSourceProvider.findRecentReviewsBySitterId(sitterId, 20)).willReturn(reviews);
        given(promptTemplateRepository.findFirstByFeatureAndCategoryAndActiveTrueOrderByIdDesc(
                "SITTER_REVIEW_SUMMARY", PromptCategory.SMALL_DOG))
                .willReturn(Optional.of(promptTemplate));
        given(aiReviewSummaryClient.generate(anyString()))
                .willReturn(new AiReviewSummaryClient.AiReviewSummaryResult(
                        response(List.of(9999L)), "stub-review-summary"));
        given(summaryRepository.findBySitterId(sitterId)).willReturn(Optional.empty());
        given(summaryRepository.save(any(SitterReviewSummary.class)))
                .willAnswer(invocation -> {
                    SitterReviewSummary saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 10L);
                    return saved;
                });

        // when
        aiReviewSummaryService.generateReviewSummary(sitterId);

        // then
        then(summaryReviewRepository).should().saveAll(argThat(usedReviews -> {
            List<SitterReviewSummaryReview> savedReviews = (List<SitterReviewSummaryReview>) usedReviews;
            List<Long> savedReviewIds = savedReviews.stream()
                    .map(SitterReviewSummaryReview::getReviewId)
                    .toList();
            return savedReviewIds.containsAll(List.of(1001L, 1002L, 1003L));
        }));
    }

    private List<ReviewSource> reviews() {
        return List.of(
                new ReviewSource(1001L, 5, "말티즈가 낯을 많이 가리는데 천천히 적응시켜주셨어요."),
                new ReviewSource(1002L, 5, "분리불안이 있는 아이를 차분하게 케어해주셨어요."),
                new ReviewSource(1003L, 4, "사진 공유가 꼼꼼했고 대형견 후기는 많지 않아 보였어요.")
        );
    }

    private AiPromptTemplate promptTemplate(PromptCategory category) {
        return AiPromptTemplate.builder()
                .feature("SITTER_REVIEW_SUMMARY")
                .category(category)
                .promptVersion("review-summary-v1")
                .template("리뷰 목록: {reviews}")
                .active(true)
                .build();
    }

    private SitterReviewSummaryResponse response(List<Long> usedReviewIds) {
        return new SitterReviewSummaryResponse(
                "소형견과 예민한 반려견을 차분하게 케어했다는 후기가 많습니다.",
                List.of("소형견 케어", "분리불안 반려견 대응"),
                List.of("대형견 후기는 아직 많지 않습니다."),
                List.of("말티즈", "분리불안 반려견"),
                List.of("소형견", "분리불안", "사진공유"),
                ReviewSentiment.POSITIVE,
                0.87,
                3,
                usedReviewIds
        );
    }
}
