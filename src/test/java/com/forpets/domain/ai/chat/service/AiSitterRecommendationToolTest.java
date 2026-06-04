package com.forpets.domain.ai.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.ai.chat.dto.AiSitterSearchCondition;
import com.forpets.domain.ai.chat.dto.RecommendedSitterDto;
import com.forpets.domain.ai.reviewsummary.entity.ReviewSentiment;
import com.forpets.domain.ai.reviewsummary.entity.SitterReviewSummary;
import com.forpets.domain.ai.reviewsummary.entity.SummaryStatus;
import com.forpets.domain.ai.reviewsummary.repository.SitterReviewSummaryRepository;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.sitter.dto.profile.SitterPageResponse;
import com.forpets.domain.sitter.dto.profile.SitterResponseDto;
import com.forpets.domain.sitter.dto.profile.SitterSearchCondition;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterApprovalStatus;
import com.forpets.domain.sitter.entity.SitterProfileStatus;
import com.forpets.domain.sitter.service.SitterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AiSitterRecommendationToolTest {

    @InjectMocks
    private AiSitterRecommendationTool recommendationTool;

    @Mock
    private SitterService sitterService;

    @Mock
    private SitterReviewSummaryRepository summaryRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("[성공] 시터 검색 결과에 저장된 AI 리뷰 요약을 붙여 추천 후보를 만든다")
    void search_sitters_with_review_summary() {
        // given
        AiSitterSearchCondition condition = new AiSitterSearchCondition(
                Region.MAPO, PossiblePetType.DOG, PossiblePetSize.SMALL, null, null, "분리불안"
        );

        given(sitterService.searchSitters(
                eq(new SitterSearchCondition(Region.MAPO, PossiblePetType.DOG, PossiblePetSize.SMALL, null, null)),
                eq(0),
                eq(3),
                eq("createdAt"),
                eq("desc")
        )).willReturn(SitterPageResponse.of(List.of(sitter()), 1, 1, 0, 3));
        given(summaryRepository.findBySitterId(1L)).willReturn(Optional.of(summary()));

        // when
        List<RecommendedSitterDto> result = recommendationTool.buildRecommendations(condition);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).reviewSummary()).contains("분리불안");
        assertThat(result.get(0).strengths()).contains("분리불안 반려견 대응");
    }

    @Test
    @DisplayName("[성공] 리뷰 요약이 없으면 Tool 실패 컨텍스트를 후보에 포함한다")
    void search_sitters_with_missing_review_summary_context() {
        // given
        AiSitterSearchCondition condition = new AiSitterSearchCondition(
                Region.MAPO, PossiblePetType.DOG, PossiblePetSize.SMALL, null, null, "분리불안"
        );

        given(sitterService.searchSitters(
                eq(new SitterSearchCondition(Region.MAPO, PossiblePetType.DOG, PossiblePetSize.SMALL, null, null)),
                eq(0),
                eq(3),
                eq("createdAt"),
                eq("desc")
        )).willReturn(SitterPageResponse.of(List.of(sitter()), 1, 1, 0, 3));
        given(summaryRepository.findBySitterId(1L)).willReturn(Optional.empty());

        // when
        List<RecommendedSitterDto> result = recommendationTool.buildRecommendations(condition);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).reviewSummary()).contains("아직 생성되지 않았습니다");
        assertThat(result.get(0).cautions()).contains("AI 리뷰 요약 생성 후 더 정확한 추천이 가능합니다.");
    }

    private SitterResponseDto sitter() {
        return new SitterResponseDto(
                1L,
                3L,
                Region.MAPO,
                "소형견 케어 경험이 많습니다.",
                3,
                PossiblePetType.DOG,
                PossiblePetSize.SMALL,
                15000,
                new BigDecimal("4.5"),
                10,
                SitterProfileStatus.RESERVABLE,
                SitterApprovalStatus.APPROVED,
                null,
                List.of(),
                null,
                null
        );
    }

    private SitterReviewSummary summary() {
        return SitterReviewSummary.builder()
                .sitterId(1L)
                .summary("분리불안 반려견을 차분하게 케어했다는 후기가 많습니다.")
                .strengths("[\"분리불안 반려견 대응\"]")
                .cautions("[]")
                .recommendedFor("[\"말티즈\"]")
                .keywords("[\"분리불안\"]")
                .sentiment(ReviewSentiment.POSITIVE)
                .confidenceScore(0.9)
                .reviewCount(3)
                .aiGenerated(true)
                .model("stub")
                .promptVersion("review-summary-v1")
                .summaryStatus(SummaryStatus.FRESH)
                .build();
    }
}
