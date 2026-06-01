package com.forpets.domain.ai.reviewsummary.service;

import com.forpets.domain.ai.reviewsummary.dto.ReviewSource;

import java.util.List;

public interface ReviewSourceProvider {

    List<ReviewSource> findRecentReviewsBySitterId(Long sitterId, int limit);
}
