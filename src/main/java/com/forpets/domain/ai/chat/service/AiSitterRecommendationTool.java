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
import com.forpets.domain.sitter.dto.schedule.ScheduleResponseDto;
import com.forpets.domain.sitter.service.SitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiSitterRecommendationTool {

    private static final int RECOMMENDATION_LIMIT = 3;

    private final SitterService sitterService;
    private final SitterReviewSummaryRepository summaryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Tool 1. 조건 기반 시터 검색.
     * LLM이 직접 DB를 상상하지 않도록 추천 후보 ID는 반드시 기존 시터 검색 API 결과에서만 가져온다.
     */
    public List<SitterResponseDto> searchSitters(AiSitterSearchCondition condition) {
        SitterSearchCondition searchCondition = new SitterSearchCondition(
                condition.region(),
                condition.possiblePetType(),
                condition.possiblePetSize(),
                condition.minPrice(),
                condition.maxPrice()
        );

        SitterPageResponse response = sitterService.searchSitters(searchCondition, 0, RECOMMENDATION_LIMIT, "createdAt");

        return response.content();
    }

    /**
     * Tool 2. 시터별 AI 리뷰 요약 조회.
     * 저장된 요약만 사용해서 추천 답변이 실제 리뷰 근거를 벗어나지 않게 한다.
     */
    public SitterReviewSummary findReviewSummary(Long sitterId) {
        return summaryRepository.findBySitterId(sitterId).orElse(null);
    }

    /**
     * Tool 3. 시터 가능 시간 조회.
     * 검색 API가 내려준 스케줄만 후보 DTO에 붙여 사용자가 바로 비교할 수 있게 한다.
     */
    public List<ScheduleResponseDto> findSchedules(SitterResponseDto sitter) {
        return sitter.schedules();
    }

    public List<RecommendedSitterDto> buildRecommendations(AiSitterSearchCondition condition) {
        return searchSitters(condition)
                .stream()
                .map(this::toRecommendedSitter)
                .toList();
    }

    private RecommendedSitterDto toRecommendedSitter(SitterResponseDto sitter) {
        SitterReviewSummary summary = findReviewSummary(sitter.id());

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
                findSchedules(sitter)
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
