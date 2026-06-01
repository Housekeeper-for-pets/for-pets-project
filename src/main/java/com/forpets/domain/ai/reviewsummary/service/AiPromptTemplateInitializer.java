package com.forpets.domain.ai.reviewsummary.service;

import com.forpets.domain.ai.reviewsummary.entity.AiPromptTemplate;
import com.forpets.domain.ai.reviewsummary.entity.PromptCategory;
import com.forpets.domain.ai.reviewsummary.repository.AiPromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "postmantest"})
@RequiredArgsConstructor
public class AiPromptTemplateInitializer implements CommandLineRunner {

    private static final String FEATURE_REVIEW_SUMMARY = "SITTER_REVIEW_SUMMARY";
    private static final String PROMPT_VERSION = "review-summary-v1";

    private final AiPromptTemplateRepository promptTemplateRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seed(PromptCategory.SMALL_DOG, smallDogTemplate());
        seed(PromptCategory.GENERAL, generalTemplate());
    }

    private void seed(PromptCategory category, String template) {
        boolean exists = promptTemplateRepository
                .findFirstByFeatureAndCategoryAndActiveTrueOrderByIdDesc(FEATURE_REVIEW_SUMMARY, category)
                .isPresent();

        if (exists) {
            return;
        }

        promptTemplateRepository.save(AiPromptTemplate.builder()
                .feature(FEATURE_REVIEW_SUMMARY)
                .category(category)
                .promptVersion(PROMPT_VERSION)
                .template(template)
                .active(true)
                .build());
    }

    private String smallDogTemplate() {
        return """
                당신은 반려동물 케어 플랫폼 ForPets의 리뷰 분석 도우미입니다.
                소형견과 예민한 반려견 케어 관점에서 아래 리뷰를 요약하세요.

                규칙:
                1. 반드시 JSON 형식으로만 응답하세요.
                2. 리뷰에 없는 자격증, 의료 행위, 전문성을 만들지 마세요.
                3. sentiment는 POSITIVE, NEUTRAL, NEGATIVE 중 하나만 사용하세요.
                4. usedReviewIds에는 입력으로 제공된 reviewId만 포함하세요.
                5. markdown 코드블록은 포함하지 마세요.

                출력 필드:
                summary, strengths, cautions, recommendedFor, keywords, sentiment, confidenceScore, reviewCount, usedReviewIds

                리뷰 목록:
                {reviews}
                """;
    }

    private String generalTemplate() {
        return """
                당신은 반려동물 케어 플랫폼 ForPets의 리뷰 분석 도우미입니다.
                아래 시터 리뷰를 보호자가 빠르게 판단할 수 있도록 사실 기반으로 요약하세요.

                규칙:
                1. 반드시 JSON 형식으로만 응답하세요.
                2. 리뷰에 근거가 없는 내용을 만들지 마세요.
                3. 부정적인 내용이 적더라도 주의점이 있다면 cautions에 포함하세요.
                4. sentiment는 POSITIVE, NEUTRAL, NEGATIVE 중 하나만 사용하세요.
                5. usedReviewIds에는 입력으로 제공된 reviewId만 포함하세요.
                6. markdown 코드블록은 포함하지 마세요.

                출력 필드:
                summary, strengths, cautions, recommendedFor, keywords, sentiment, confidenceScore, reviewCount, usedReviewIds

                리뷰 목록:
                {reviews}
                """;
    }
}
