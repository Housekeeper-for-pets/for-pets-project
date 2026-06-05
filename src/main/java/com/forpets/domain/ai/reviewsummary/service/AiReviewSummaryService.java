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
import com.forpets.domain.ai.usage.entity.AiErrorType;
import com.forpets.domain.ai.usage.entity.AiFeature;
import com.forpets.domain.ai.usage.service.AiUsageLogService;
import com.forpets.domain.ai.usage.service.AiUsageRecord;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AiReviewSummaryService {

    private static final String FEATURE_REVIEW_SUMMARY = "SITTER_REVIEW_SUMMARY";
    private static final String FALLBACK_MODEL = "fallback-review-summary";
    private static final int REVIEW_LIMIT = 20;

    private final SitterProfileRepository sitterProfileRepository;
    private final SitterReviewSummaryRepository summaryRepository;
    private final SitterReviewSummaryReviewRepository summaryReviewRepository;
    private final AiPromptTemplateRepository promptTemplateRepository;
    private final ReviewSourceProvider reviewSourceProvider;
    private final AiReviewSummaryClient aiReviewSummaryClient;
    private final AiUsageLogService aiUsageLogService;
    private final ObjectMapper objectMapper;

    @Transactional
    public SitterReviewSummaryDto generateReviewSummary(Long sitterId) {
        long startedAt = System.nanoTime();
        validateSitterExists(sitterId);

        List<ReviewSource> reviews = reviewSourceProvider.findRecentReviewsBySitterId(sitterId, REVIEW_LIMIT);
        if (reviews.isEmpty()) {
            throw new AiReviewSummaryException(AiReviewSummaryErrorCode.REVIEW_SOURCE_NOT_FOUND);
        }

        PromptCategory category = resolveCategory(reviews);
        AiPromptTemplate promptTemplate = findPromptTemplate(category);
        String prompt = buildPrompt(promptTemplate.getTemplate(), reviews);

        // AI 호출이 실패해도 상세/챗봇 화면이 비지 않도록, 검증 가능한 리뷰 원문 기반 fallback 요약을 저장한다.
        AiReviewSummaryClient.AiReviewSummaryResult result = generateWithFallback(sitterId, prompt, reviews);
        ValidatedSummary validatedSummary = validateOrFallback(sitterId, result, reviews);
        recordUsage(result, validatedSummary, promptTemplate, elapsedMs(startedAt));
        SitterReviewSummaryResponse response = validatedSummary.response();

        SitterReviewSummary generated = toEntity(sitterId, response, validatedSummary.model(), promptTemplate);
        SitterReviewSummary saved = saveOrReplace(sitterId, generated);
        replaceUsedReviews(saved.getId(), response.usedReviewIds());

        return SitterReviewSummaryDto.from(saved, objectMapper);
    }

    private AiReviewSummaryClient.AiReviewSummaryResult generateWithFallback(
            Long sitterId,
            String prompt,
            List<ReviewSource> reviews
    ) {
        try {
            return aiReviewSummaryClient.generate(prompt);
        } catch (AiReviewSummaryException exception) {
            log.warn("AI 리뷰 요약 생성 실패. fallback 요약을 저장합니다. sitterId={}, code={}",
                    sitterId, exception.getErrorCode().getCode());
            return new AiReviewSummaryClient.AiReviewSummaryResult(
                    buildFallbackResponse(reviews),
                    FALLBACK_MODEL
            );
        }
    }

    private ValidatedSummary validateOrFallback(
            Long sitterId,
            AiReviewSummaryClient.AiReviewSummaryResult result,
            List<ReviewSource> reviews
    ) {
        try {
            return new ValidatedSummary(validateAndNormalizeAiResponse(result.response(), reviews), result.model());
        } catch (AiReviewSummaryException exception) {
            log.warn("AI 리뷰 요약 응답 검증 실패. fallback 요약을 저장합니다. sitterId={}, code={}",
                    sitterId, exception.getErrorCode().getCode());
            return new ValidatedSummary(
                    buildFallbackResponse(reviews),
                    FALLBACK_MODEL,
                    AiErrorType.INVALID_RESPONSE,
                    exception.getErrorCode().getCode()
            );
        }
    }

    private void recordUsage(
            AiReviewSummaryClient.AiReviewSummaryResult result,
            ValidatedSummary validatedSummary,
            AiPromptTemplate promptTemplate,
            long latencyMs
    ) {
        if (!FALLBACK_MODEL.equals(validatedSummary.model())) {
            aiUsageLogService.record(AiUsageRecord.success(
                    AiFeature.SITTER_REVIEW_SUMMARY,
                    result.model(),
                    result.promptTokens(),
                    result.completionTokens(),
                    result.totalTokens(),
                    latencyMs,
                    promptTemplate.getPromptVersion()
            ));
            return;
        }

        AiErrorType errorType = validatedSummary.errorType() == null
                ? AiErrorType.UNKNOWN
                : validatedSummary.errorType();
        String failureReason = validatedSummary.failureReason() == null
                ? "AI_REVIEW_SUMMARY_FALLBACK"
                : validatedSummary.failureReason();

        aiUsageLogService.record(AiUsageRecord.fallback(
                AiFeature.SITTER_REVIEW_SUMMARY,
                result.model(),
                latencyMs,
                errorType,
                promptTemplate.getPromptVersion(),
                failureReason
        ));
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
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

    private SitterReviewSummaryResponse validateAndNormalizeAiResponse(SitterReviewSummaryResponse response, List<ReviewSource> reviews) {
        if (!StringUtils.hasText(response.summary())
                || response.sentiment() == null
                || response.confidenceScore() == null
                || response.confidenceScore() < 0
                || response.confidenceScore() > 1
                || response.reviewCount() == null
                || response.strengths() == null
                || response.cautions() == null
                || response.recommendedFor() == null
                || response.keywords() == null
                || response.usedReviewIds() == null
                || response.usedReviewIds().isEmpty()) {
            throw new AiReviewSummaryException(AiReviewSummaryErrorCode.INVALID_AI_RESPONSE);
        }

        List<Long> sourceReviewIds = reviews.stream()
                .map(ReviewSource::reviewId)
                .toList();
        Set<Long> sourceReviewIdSet = Set.copyOf(sourceReviewIds);

        List<Long> normalizedUsedReviewIds = response.usedReviewIds().stream()
                .filter(sourceReviewIdSet::contains)
                .distinct()
                .toList();

        if (containsForbiddenExpression(response)) {
            throw new AiReviewSummaryException(AiReviewSummaryErrorCode.INVALID_AI_RESPONSE, "근거 없는 의료/자격 표현이 포함되어 있습니다.");
        }

        if (normalizedUsedReviewIds.size() == response.usedReviewIds().size()) {
            return response;
        }

        // 모델이 reviewId를 잘못 생성한 경우에도 요약 근거는 입력 리뷰 목록으로 제한한다.
        return new SitterReviewSummaryResponse(
                response.summary(),
                response.strengths(),
                response.cautions(),
                response.recommendedFor(),
                response.keywords(),
                response.sentiment(),
                response.confidenceScore(),
                response.reviewCount(),
                sourceReviewIds
        );
    }

    private boolean containsForbiddenExpression(SitterReviewSummaryResponse response) {
        return containsForbiddenExpression(response.summary())
                || containsForbiddenExpression(response.strengths())
                || containsForbiddenExpression(response.cautions())
                || containsForbiddenExpression(response.recommendedFor())
                || containsForbiddenExpression(response.keywords());
    }

    private boolean containsForbiddenExpression(List<String> values) {
        return values != null && values.stream().anyMatch(this::containsForbiddenExpression);
    }

    private boolean containsForbiddenExpression(String value) {
        return StringUtils.hasText(value)
                && (value.contains("진단")
                || value.contains("치료")
                || value.contains("수의사")
                || value.contains("자격증 보유"));
    }

    private SitterReviewSummaryResponse buildFallbackResponse(List<ReviewSource> reviews) {
        List<Long> reviewIds = reviews.stream()
                .map(ReviewSource::reviewId)
                .toList();
        double averageRating = reviews.stream()
                .map(ReviewSource::rating)
                .filter(rating -> rating != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        List<String> strengths = resolveFallbackStrengths(reviews);
        String summary = "최근 보호자 리뷰를 기준으로 "
                + String.join(", ", strengths)
                + " 부분이 자주 언급되었습니다.";

        return new SitterReviewSummaryResponse(
                summary,
                strengths,
                List.of("AI 요약 생성에 실패하여 리뷰 원문 기반 기본 요약을 제공합니다."),
                List.of("리뷰 내용을 직접 확인해보세요."),
                strengths,
                averageRating >= 4.0 ? ReviewSentiment.POSITIVE : ReviewSentiment.NEUTRAL,
                0.5,
                reviews.size(),
                reviewIds
        );
    }

    private List<String> resolveFallbackStrengths(List<ReviewSource> reviews) {
        String joined = reviews.stream()
                .map(ReviewSource::content)
                .collect(Collectors.joining(" "));

        List<String> strengths = new java.util.ArrayList<>();
        if (joined.contains("사진")) {
            strengths.add("꼼꼼한 사진 공유");
        }
        if (joined.contains("응답") || joined.contains("빠르")) {
            strengths.add("빠른 보호자 소통");
        }
        if (joined.contains("분리불안") || joined.contains("낯가림")) {
            strengths.add("예민한 반려동물 케어");
        }
        if (joined.contains("소형") || joined.contains("말티즈") || joined.contains("포메라니안")) {
            strengths.add("소형견 케어");
        }

        return strengths.isEmpty() ? List.of("전반적인 케어 만족도") : strengths;
    }

    private SitterReviewSummary toEntity(Long sitterId, SitterReviewSummaryResponse response, String model,
                                         AiPromptTemplate promptTemplate) {
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
                .aiGenerated(!FALLBACK_MODEL.equals(model))
                .model(model)
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
        summaryReviewRepository.flush();

        List<SitterReviewSummaryReview> usedReviews = reviewIds.stream()
                .distinct()
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

    private record ValidatedSummary(
            SitterReviewSummaryResponse response,
            String model,
            AiErrorType errorType,
            String failureReason
    ) {
        private ValidatedSummary(SitterReviewSummaryResponse response, String model) {
            this(response, model, null, null);
        }
    }
}
