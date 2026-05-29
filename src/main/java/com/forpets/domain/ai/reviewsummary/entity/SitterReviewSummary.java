package com.forpets.domain.ai.reviewsummary.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "sitter_review_summaries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sitter_review_summary_sitter", columnNames = "sitter_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SitterReviewSummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sitter_id", nullable = false)
    private Long sitterId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String strengths;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String cautions;

    @Column(name = "recommended_for", nullable = false, columnDefinition = "TEXT")
    private String recommendedFor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String keywords;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewSentiment sentiment;

    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "prompt_version", nullable = false, length = 30)
    private String promptVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "summary_status", nullable = false, length = 20)
    private SummaryStatus summaryStatus;

    @Builder
    private SitterReviewSummary(Long sitterId, String summary, String strengths, String cautions,
                                String recommendedFor, String keywords, ReviewSentiment sentiment,
                                Double confidenceScore, Integer reviewCount, boolean aiGenerated,
                                String model, String promptVersion, SummaryStatus summaryStatus) {
        this.sitterId = sitterId;
        this.summary = summary;
        this.strengths = strengths;
        this.cautions = cautions;
        this.recommendedFor = recommendedFor;
        this.keywords = keywords;
        this.sentiment = sentiment;
        this.confidenceScore = confidenceScore;
        this.reviewCount = reviewCount;
        this.aiGenerated = aiGenerated;
        this.model = model;
        this.promptVersion = promptVersion;
        this.summaryStatus = summaryStatus;
    }

    public void replaceWith(SitterReviewSummary summary) {
        this.summary = summary.summary;
        this.strengths = summary.strengths;
        this.cautions = summary.cautions;
        this.recommendedFor = summary.recommendedFor;
        this.keywords = summary.keywords;
        this.sentiment = summary.sentiment;
        this.confidenceScore = summary.confidenceScore;
        this.reviewCount = summary.reviewCount;
        this.aiGenerated = summary.aiGenerated;
        this.model = summary.model;
        this.promptVersion = summary.promptVersion;
        this.summaryStatus = summary.summaryStatus;
    }

    public void markStale() {
        this.summaryStatus = SummaryStatus.STALE;
    }
}
