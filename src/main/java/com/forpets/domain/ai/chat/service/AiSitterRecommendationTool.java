package com.forpets.domain.ai.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.ai.chat.dto.AiSitterSearchCondition;
import com.forpets.domain.ai.chat.dto.RecommendedSitterDto;
import com.forpets.domain.ai.reviewsummary.entity.SitterReviewSummary;
import com.forpets.domain.ai.reviewsummary.entity.SummaryStatus;
import com.forpets.domain.ai.reviewsummary.repository.SitterReviewSummaryRepository;
import com.forpets.domain.sitter.dto.profile.SitterPageResponse;
import com.forpets.domain.sitter.dto.profile.SitterResponseDto;
import com.forpets.domain.sitter.dto.profile.SitterSearchCondition;
import com.forpets.domain.sitter.dto.schedule.ScheduleResponseDto;
import com.forpets.domain.sitter.service.SitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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
    @Tool(name = "searchSitters", description = "지역, 반려동물 종류/크기, 가격 조건에 맞는 실제 펫시터 후보를 검색합니다.")
    public List<SitterResponseDto> searchSitters(
            @ToolParam(description = "사용자 자연어 요청에서 추출한 시터 검색 조건") AiSitterSearchCondition condition
    ) {
        SitterSearchCondition searchCondition = new SitterSearchCondition(
                condition.region(),
                condition.possiblePetType(),
                condition.possiblePetSize(),
                condition.minPrice(),
                condition.maxPrice()
        );

        SitterPageResponse response = sitterService.searchSitters(searchCondition, 0, RECOMMENDATION_LIMIT, "createdAt", "desc");

        return response.content();
    }

    /**
     * Tool 2. 시터별 AI 리뷰 요약 조회.
     * Tool 실패/미생성 상태도 문자열 컨텍스트로 반환해 LLM이 사용자에게 자연스럽게 안내할 수 있게 한다.
     */
    @Tool(name = "getSitterReviewSummary", description = "특정 시터의 저장된 AI 리뷰 요약과 조회 상태를 반환합니다.")
    public ReviewSummaryToolResult getSitterReviewSummary(
            @ToolParam(description = "리뷰 요약을 조회할 시터 프로필 ID") Long sitterId
    ) {
        try {
            return summaryRepository.findBySitterId(sitterId)
                    .map(this::toReviewSummaryToolResult)
                    .orElseGet(() -> ReviewSummaryToolResult.missing("리뷰 요약이 아직 생성되지 않았습니다."));
        } catch (Exception exception) {
            return ReviewSummaryToolResult.failed("리뷰 요약 조회에 실패했습니다. 시터 기본 정보와 가능 시간만으로 안내해주세요.");
        }
    }

    /**
     * Tool 3. 시터 가능 시간 조회.
     * 검색 API가 내려준 스케줄만 후보 DTO에 붙여 사용자가 바로 비교할 수 있게 한다.
     */
    @Tool(name = "getCandidateSchedules", description = "시터 검색 결과 후보에 포함된 가능 시간 목록을 반환합니다.")
    public List<ScheduleResponseDto> findSchedules(
            @ToolParam(description = "시터 검색 API가 반환한 후보 시터 정보") SitterResponseDto sitter
    ) {
        return sitter.schedules();
    }

    public List<RecommendedSitterDto> buildRecommendations(AiSitterSearchCondition condition) {
        return searchSitters(condition)
                .stream()
                .map(this::toRecommendedSitter)
                .toList();
    }

    private RecommendedSitterDto toRecommendedSitter(SitterResponseDto sitter) {
        ReviewSummaryToolResult summary = getSitterReviewSummary(sitter.id());

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
                summary.summary(),
                summary.strengths(),
                summary.cautions(),
                findSchedules(sitter)
        );
    }

    private ReviewSummaryToolResult toReviewSummaryToolResult(SitterReviewSummary summary) {
        List<String> cautions = new java.util.ArrayList<>(readList(summary.getCautions()));
        if (summary.getSummaryStatus() == SummaryStatus.STALE) {
            cautions.add("최근 리뷰 변경으로 요약 갱신이 필요합니다.");
        }

        return new ReviewSummaryToolResult(
                summary.getSummaryStatus().name(),
                summary.getSummary(),
                readList(summary.getStrengths()),
                cautions
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

    public record ReviewSummaryToolResult(
            String status,
            String summary,
            List<String> strengths,
            List<String> cautions
    ) {

        private static ReviewSummaryToolResult missing(String message) {
            return new ReviewSummaryToolResult(
                    "MISSING",
                    message,
                    List.of(),
                    List.of("AI 리뷰 요약 생성 후 더 정확한 추천이 가능합니다.")
            );
        }

        private static ReviewSummaryToolResult failed(String message) {
            return new ReviewSummaryToolResult(
                    "FAILED",
                    message,
                    List.of(),
                    List.of("리뷰 요약 Tool 호출 실패")
            );
        }
    }
}
