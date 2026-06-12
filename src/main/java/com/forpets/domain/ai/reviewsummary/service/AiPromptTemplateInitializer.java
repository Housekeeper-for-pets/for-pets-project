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
@Profile({"local", "postmantest", "ai"})
@RequiredArgsConstructor
public class AiPromptTemplateInitializer implements CommandLineRunner {

    private static final String FEATURE_REVIEW_SUMMARY = "SITTER_REVIEW_SUMMARY";
    private static final String PROMPT_VERSION = "review-summary-v3";

    private final AiPromptTemplateRepository promptTemplateRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seed(PromptCategory.SMALL_DOG, smallDogTemplate());
        seed(PromptCategory.SENIOR_DOG, seniorDogTemplate());
        seed(PromptCategory.MEDICAL_CARE, medicalCareTemplate());
        seed(PromptCategory.GENERAL, generalTemplate());
    }

    private void seed(PromptCategory category, String template) {
        boolean latestVersionExists = promptTemplateRepository
                .findFirstByFeatureAndCategoryAndActiveTrueOrderByIdDesc(FEATURE_REVIEW_SUMMARY, category)
                .filter(promptTemplate -> PROMPT_VERSION.equals(promptTemplate.getPromptVersion()))
                .isPresent();

        if (latestVersionExists) {
            return;
        }

        // 기존 v1 템플릿이 DB에 남아 있어도 최신 버전을 새로 저장해 재배포 없이 프롬프트를 갱신한다.
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
                6. 아래 JSON 객체 구조와 camelCase 필드명을 그대로 사용하세요.

                출력 JSON:
                {
                  "summary": "string",
                  "strengths": ["string"],
                  "cautions": ["string"],
                  "recommendedFor": ["string"],
                  "keywords": ["string"],
                  "sentiment": "POSITIVE | NEUTRAL | NEGATIVE",
                  "confidenceScore": 0.0,
                  "reviewCount": 0,
                  "usedReviewIds": [1]
                }

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
                7. 아래 JSON 객체 구조와 camelCase 필드명을 그대로 사용하세요.

                출력 JSON:
                {
                  "summary": "string",
                  "strengths": ["string"],
                  "cautions": ["string"],
                  "recommendedFor": ["string"],
                  "keywords": ["string"],
                  "sentiment": "POSITIVE | NEUTRAL | NEGATIVE",
                  "confidenceScore": 0.0,
                  "reviewCount": 0,
                  "usedReviewIds": [1]
                }

                리뷰 목록:
                {reviews}
                """;
    }

    private String seniorDogTemplate() {
        return """
                당신은 반려동물 케어 플랫폼 ForPets의 리뷰 분석 도우미입니다.
                노령견 또는 시니어 반려동물 케어 관점에서 아래 리뷰를 요약하세요.

                규칙:
                1. 반드시 JSON 형식으로만 응답하세요.
                2. 리뷰에 없는 의료 판단, 치료 가능 여부, 자격증 보유 여부를 만들지 마세요.
                3. 느린 산책, 안정감, 관찰, 보호자 소통처럼 리뷰에 근거가 있는 케어 경험만 언급하세요.
                4. sentiment는 POSITIVE, NEUTRAL, NEGATIVE 중 하나만 사용하세요.
                5. usedReviewIds에는 입력으로 제공된 reviewId만 포함하세요.
                6. markdown 코드블록은 포함하지 마세요.
                7. 아래 JSON 객체 구조와 camelCase 필드명을 그대로 사용하세요.

                출력 JSON:
                {
                  "summary": "string",
                  "strengths": ["string"],
                  "cautions": ["string"],
                  "recommendedFor": ["string"],
                  "keywords": ["string"],
                  "sentiment": "POSITIVE | NEUTRAL | NEGATIVE",
                  "confidenceScore": 0.0,
                  "reviewCount": 0,
                  "usedReviewIds": [1]
                }

                리뷰 목록:
                {reviews}
                """;
    }

    private String medicalCareTemplate() {
        return """
                당신은 반려동물 케어 플랫폼 ForPets의 리뷰 분석 도우미입니다.
                투약 보조나 병원 동행처럼 보호자 리뷰에 직접 언급된 특수 케어 경험을 사실 기반으로 요약하세요.

                규칙:
                1. 반드시 JSON 형식으로만 응답하세요.
                2. 수의학적 진단, 치료, 의료 행위 가능 여부, 자격증 보유 여부를 만들지 마세요.
                3. 리뷰에 적힌 보호자 소통, 주의사항 전달, 시간 준수, 관찰 내용만 언급하세요.
                4. 의료적 판단처럼 보일 수 있는 표현은 cautions에 주의 문구로 처리하세요.
                5. sentiment는 POSITIVE, NEUTRAL, NEGATIVE 중 하나만 사용하세요.
                6. usedReviewIds에는 입력으로 제공된 reviewId만 포함하세요.
                7. markdown 코드블록은 포함하지 마세요.
                8. 아래 JSON 객체 구조와 camelCase 필드명을 그대로 사용하세요.

                출력 JSON:
                {
                  "summary": "string",
                  "strengths": ["string"],
                  "cautions": ["string"],
                  "recommendedFor": ["string"],
                  "keywords": ["string"],
                  "sentiment": "POSITIVE | NEUTRAL | NEGATIVE",
                  "confidenceScore": 0.0,
                  "reviewCount": 0,
                  "usedReviewIds": [1]
                }

                리뷰 목록:
                {reviews}
                """;
    }
}
