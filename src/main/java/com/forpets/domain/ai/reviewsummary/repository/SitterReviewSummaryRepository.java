package com.forpets.domain.ai.reviewsummary.repository;

import com.forpets.domain.ai.reviewsummary.entity.SitterReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SitterReviewSummaryRepository extends JpaRepository<SitterReviewSummary, Long> {

    Optional<SitterReviewSummary> findBySitterId(Long sitterId);
}
