package com.forpets.domain.ai.reviewsummary.controller;

import com.forpets.domain.ai.reviewsummary.dto.SitterReviewSummaryDto;
import com.forpets.domain.ai.reviewsummary.service.AiReviewSummaryService;
import com.forpets.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AiReviewSummaryController {

    private final AiReviewSummaryService aiReviewSummaryService;

    @PostMapping("/api/ai/sitters/{sitterId}/review-summary")
    public ResponseEntity<ApiResponse<SitterReviewSummaryDto>> generateReviewSummary(
            @PathVariable Long sitterId) {
        return ResponseEntity.ok(ApiResponse.success(aiReviewSummaryService.generateReviewSummary(sitterId)));
    }

    @GetMapping("/api/sitters/{sitterId}/review-summary")
    public ResponseEntity<ApiResponse<SitterReviewSummaryDto>> getReviewSummary(
            @PathVariable Long sitterId) {
        return ResponseEntity.ok(ApiResponse.success(aiReviewSummaryService.getReviewSummary(sitterId)));
    }
}
