package com.forpets.domain.review.controller;

import com.forpets.domain.review.dto.CreateReviewRequest;
import com.forpets.domain.review.dto.ReviewPageResponse;
import com.forpets.domain.review.dto.ReviewResponse;
import com.forpets.domain.review.service.ReviewService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(reviewService.create(currentMember.id(), request)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long reviewId) {
        reviewService.delete(currentMember.id(), reviewId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/sitters/{sitterId}")
    public ResponseEntity<ApiResponse<ReviewPageResponse>> getSitterReviews(
            @PathVariable Long sitterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getSitterReviews(sitterId, page, size, sort, direction)));
    }
}
