package com.forpets.domain.ai.reviewsummary.repository;

import com.forpets.domain.ai.reviewsummary.entity.SitterReviewSummaryReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SitterReviewSummaryReviewRepository extends JpaRepository<SitterReviewSummaryReview, Long> {

    void deleteAllBySummaryId(Long summaryId);
}
