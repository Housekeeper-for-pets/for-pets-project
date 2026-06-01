package com.forpets.domain.ai.reviewsummary.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReviewSummaryService {

    private static final String FEATURE_REVIEW_SUMMARY = "SITTER_REVIEW_SUMMARY";
    private static final int REVIEW_LIMIT = 20;

    private final SitterProfileRepository sitterProfileRepository;
    private final SitterReviewSummaryRepository summaryRepository;
    private final SitterReviewSummaryReviewRepository summaryReviewRepository;
    private final AiPromptTemplateRepository promptTemplateRepository;
    private final ReviewSourceProvider reviewSourceProvider;
    private final AiReviewSummaryClient aiReviewSummaryClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public SitterReviewSummaryDto generateReviewSummary(Long sitterId) {
        validateSitterExists(sitterId);

        List<ReviewSource> reviews = reviewSourceProvider.findRecentReviewsBySitterId(sitterId, REVIEW_LIMIT);
        if (reviews.isEmpty()) {
            throw new AiReviewSummaryException(AiReviewSummaryErrorCode.REVIEW_SOURCE_NOT_FOUND);
        }

        PromptCategory category = resolveCategory(reviews);
        AiPromptTemplate promptTemplate = findPromptTemplate(category);
        String prompt = buildPrompt(promptTemplate.getTemplate(), reviews);

        // AI 응답은 저장 전에 스키마와 근거 리뷰 ID를 검증해 환각성 데이터를 막는다.
        AiReviewSummaryClient.AiReviewSummaryResult result = aiReviewSummaryClient.generate(prompt);
        validateAiResponse(result.response(), reviews);

        SitterReviewSummary generated = toEntity(sitterId, result, promptTemplate);
        SitterReviewSummary saved = saveOrReplace(sitterId, generated);
        replaceUsedReviews(saved.getId(), result.response().usedReviewIds());

        return SitterReviewSummaryDto.from(saved, objectMapper);
    }

    public SitterReviewSummaryDto getReviewSummary(Long sitterId) {
        SitterReviewSummary summary = summaryRepository.findBySitterId(sitterId)
                .orElseThrow(() -> new AiReviewSummaryException(AiReviewSummaryErrorCode.REVIEW_SUMMARY_NOT_FOUND));

        return SitterReviewSummaryDto.from(summary, objectMapper);
    }

    private void validateSitterExists(Long sitterId) {
        if (!sitterProfileRepository.existsById(sitterId)) {
            throw new SitterException(SitterErrorCode.SITTER_NOT_FOUND);
        }
    }

    private AiPromptTemplate findPromptTemplate(PromptCategory category) {
        return promptTemplateRepository
                .findFirstByFeatureAndCategoryAndActiveTrueOrderByIdDesc(FEATURE_REVIEW_SUMMARY, category)
                .or(() -> promptTemplateRepository.findFirstByFeatureAndCategoryAndActiveTrueOrderByIdDesc(
                        FEATURE_REVIEW_SUMMARY, PromptCategory.GENERAL))
                .orElseThrow(() -> new AiReviewSummaryException(AiReviewSummaryErrorCode.PROMPT_TEMPLATE_NOT_FOUND));
    }

    private PromptCategory resolveCategory(List<ReviewSource> reviews) {
        String joined = reviews.stream()
                .map(ReviewSource::content)
                .collect(Collectors.joining(" "));

        if (joined.contains("투약") || joined.contains("약") || joined.contains("병원")) {
            return PromptCategory.MEDICAL_CARE;
        }
        if (joined.contains("노령") || joined.contains("노견") || joined.contains("시니어")) {
            return PromptCategory.SENIOR_DOG;
        }
        if (joined.contains("말티즈") || joined.contains("포메라니안") || joined.contains("소형")) {
            return PromptCategory.SMALL_DOG;
        }
        return PromptCategory.GENERAL;
    }

    private String buildPrompt(String template, List<ReviewSource> reviews) {
        return template.replace("{reviews}", toJson(reviews));
    }

    private void validateAiResponse(SitterReviewSummaryResponse response, List<ReviewSource> reviews) {
        if (!StringUtils.hasText(response.summary())
                || response.sentiment() == null
                || response.confidenceScore() == null
                || response.confidenceScore() < 0
                || response.confidenceScore() > 1
                || response.usedReviewIds() == null
                || response.usedReviewIds().isEmpty()) {
            throw new AiReviewSummaryException(AiReviewSummaryErrorCode.INVALID_AI_RESPONSE);
        }

        Set<Long> sourceReviewIds = reviews.stream()
                .map(ReviewSource::reviewId)
                .collect(Collectors.toSet());

        if (!sourceReviewIds.containsAll(response.usedReviewIds())) {
            throw new AiReviewSummaryException(AiReviewSummaryErrorCode.INVALID_AI_RESPONSE, "요약에 사용된 리뷰 ID가 입력 리뷰 목록과 일치하지 않습니다.");
        }

        if (containsForbiddenExpression(response.summary())) {
            throw new AiReviewSummaryException(AiReviewSummaryErrorCode.INVALID_AI_RESPONSE, "근거 없는 의료/자격 표현이 포함되어 있습니다.");
        }
    }

    private boolean containsForbiddenExpression(String value) {
        return value.contains("진단")
                || value.contains("치료")
                || value.contains("수의사")
                || value.contains("자격증 보유");
    }

    private SitterReviewSummary toEntity(Long sitterId, AiReviewSummaryClient.AiReviewSummaryResult result,
                                         AiPromptTemplate promptTemplate) {
        SitterReviewSummaryResponse response = result.response();
        return SitterReviewSummary.builder()
                .sitterId(sitterId)
                .summary(response.summary())
                .strengths(toJson(response.strengths()))
                .cautions(toJson(response.cautions()))
                .recommendedFor(toJson(response.recommendedFor()))
                .keywords(toJson(response.keywords()))
                .sentiment(response.sentiment())
                .confidenceScore(response.confidenceScore())
                .reviewCount(response.reviewCount())
                .aiGenerated(true)
                .model(result.model())
                .promptVersion(promptTemplate.getPromptVersion())
                .summaryStatus(SummaryStatus.FRESH)
                .build();
    }

    private SitterReviewSummary saveOrReplace(Long sitterId, SitterReviewSummary generated) {
        return summaryRepository.findBySitterId(sitterId)
                .map(saved -> {
                    saved.replaceWith(generated);
                    return saved;
                })
                .orElseGet(() -> summaryRepository.save(generated));
    }

    private void replaceUsedReviews(Long summaryId, List<Long> reviewIds) {
        summaryReviewRepository.deleteAllBySummaryId(summaryId);
        List<SitterReviewSummaryReview> usedReviews = reviewIds.stream()
                .map(reviewId -> SitterReviewSummaryReview.builder()
                        .summaryId(summaryId)
                        .reviewId(reviewId)
                        .build())
                .toList();
        summaryReviewRepository.saveAll(usedReviews);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new AiReviewSummaryException(AiReviewSummaryErrorCode.AI_REVIEW_SUMMARY_FAILED);
        }
    }
}
