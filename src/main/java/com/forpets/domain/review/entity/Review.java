package com.forpets.domain.review.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "review",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_reservation", columnNames = "reservation_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "reviewee_id", nullable = false)
    private Long revieweeId;

    @Column(name = "review_comment", nullable = false, columnDefinition = "TEXT")
    private String reviewComment;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false)
    private boolean deleted = false;

    @Builder
    private Review(Long reservationId, Long reviewerId, Long revieweeId, String reviewComment, Integer rating) {
        this.reservationId = reservationId;
        this.reviewerId = reviewerId;
        this.revieweeId = revieweeId;
        this.reviewComment = reviewComment;
        this.rating = rating;
        this.deleted = false;
    }

    public void delete() {
        this.deleted = true;
    }
}
