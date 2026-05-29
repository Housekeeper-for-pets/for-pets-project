package com.forpets.domain.ai.reviewsummary.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "sitter_review_summary_reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_summary_review", columnNames = {"summary_id", "review_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SitterReviewSummaryReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "summary_id", nullable = false)
    private Long summaryId;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Builder
    private SitterReviewSummaryReview(Long summaryId, Long reviewId) {
        this.summaryId = summaryId;
        this.reviewId = reviewId;
    }
}
