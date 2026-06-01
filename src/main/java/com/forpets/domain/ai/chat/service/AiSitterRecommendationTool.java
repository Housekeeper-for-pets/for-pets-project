package com.forpets.domain.ai.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.ai.chat.dto.AiSitterSearchCondition;
import com.forpets.domain.ai.chat.dto.RecommendedSitterDto;
import com.forpets.domain.ai.reviewsummary.entity.SitterReviewSummary;
import com.forpets.domain.ai.reviewsummary.repository.SitterReviewSummaryRepository;
import com.forpets.domain.sitter.dto.profile.SitterPageResponse;
import com.forpets.domain.sitter.dto.profile.SitterResponseDto;
import com.forpets.domain.sitter.dto.profile.SitterSearchCondition;
import com.forpets.domain.sitter.service.SitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiSitterRecommendationTool {

    private static final int RECOMMENDATION_LIMIT = 5;

    private final SitterService sitterService;
    private final SitterReviewSummaryRepository summaryRepository;
    private final ObjectMapper objectMapper;

    /**
     * LLM이 직접 DB를 상상하지 않도록, 추천 후보는 반드시 기존 시터 검색 API 결과에서만 만든다.
     * 추후 Spring AI @Tool 도입 시 이 메서드를 Tool 메서드로 그대로 노출할 수 있다.
     */
    public List<RecommendedSitterDto> searchSitters(AiSitterSearchCondition condition) {
        SitterSearchCondition searchCondition = new SitterSearchCondition(
                condition.region(),
                condition.possiblePetType(),
                condition.possiblePetSize(),
                condition.minPrice(),
                condition.maxPrice()
        );

        SitterPageResponse response = sitterService.searchSitters(searchCondition, 0, RECOMMENDATION_LIMIT, "createdAt");

        return response.content()
                .stream()
                .map(this::toRecommendedSitter)
                .toList();
    }

    private RecommendedSitterDto toRecommendedSitter(SitterResponseDto sitter) {
        SitterReviewSummary summary = summaryRepository.findBySitterId(sitter.id()).orElse(null);

        return new RecommendedSitterDto(
                sitter.id(),
                sitter.memberId(),
                sitter.region(),
                sitter.introduction(),
                sitter.experienceYears(),
                sitter.possiblePetType(),
                sitter.possiblePetSize(),
                sitter.pricePerHour(),
                sitter.status(),
                summary == null ? null : summary.getSummary(),
                summary == null ? List.of() : readList(summary.getStrengths()),
                summary == null ? List.of() : readList(summary.getCautions()),
                sitter.schedules()
        );
    }

    private List<String> readList(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }
}
