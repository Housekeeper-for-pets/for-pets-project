package com.forpets.domain.sitter.entity;

import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "sitter_profile", uniqueConstraints = {
        @UniqueConstraint(columnNames = "member_id")
})
@SQLRestriction("deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SitterProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(nullable = false)
    private int experienceYears;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PossiblePetType possiblePetType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PossiblePetSize possiblePetSize;

    @Column(nullable = false)
    private int pricePerHour;

    @Column(name = "average_rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SitterProfileStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SitterApprovalStatus approvalStatus;

    @Column(length = 500)
    private String rejectReason;

    @Column(name = "evaluated_by")
    private Long evaluatedBy;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private SitterProfile(Long memberId, String introduction,
                          int experienceYears, PossiblePetType possiblePetType,
                          PossiblePetSize possiblePetSize, int pricePerHour) {
        this.memberId = memberId;
        this.introduction = introduction;
        this.experienceYears = experienceYears;
        this.possiblePetType = possiblePetType;
        this.possiblePetSize = possiblePetSize == null ? PossiblePetSize.ALL : possiblePetSize;
        this.pricePerHour = pricePerHour;
        this.status = SitterProfileStatus.NON_RESERVABLE;
        this.approvalStatus = SitterApprovalStatus.PENDING;
    }

    public void update(String introduction, int experienceYears,
                       PossiblePetType possiblePetType, PossiblePetSize possiblePetSize, int pricePerHour) {
        this.introduction = introduction;
        this.experienceYears = experienceYears;
        this.possiblePetType = possiblePetType;
        this.possiblePetSize = possiblePetSize;
        this.pricePerHour = pricePerHour;
    }

    public void changeStatus(SitterProfileStatus status) {
        this.status = status;
    }

    /**
     * 리뷰 작성/삭제 시 평균 평점과 리뷰 수를 갱신합니다.
     * 증분이 아니라 매번 전체 재계산한 값으로 덮어씁니다. (동시성 이슈 방지)
     */
    public void updateReviewStats(BigDecimal averageRating, int reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    public void approve(Long adminId) {
        this.approvalStatus = SitterApprovalStatus.APPROVED;
        this.rejectReason = null;
        this.evaluatedBy = adminId;
        this.evaluatedAt = LocalDateTime.now();
        this.status = SitterProfileStatus.RESERVABLE;
    }

    public void reject(Long adminId, String rejectReason) {
        this.approvalStatus = SitterApprovalStatus.REJECTED;
        this.rejectReason = rejectReason;
        this.evaluatedBy = adminId;
        this.evaluatedAt = LocalDateTime.now();
    }


    public void delete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void reactivate() {
        this.deleted = false;
        this.deletedAt = null;
        this.approvalStatus = SitterApprovalStatus.PENDING;
        this.status = SitterProfileStatus.NON_RESERVABLE;
        this.rejectReason = null;
        this.evaluatedBy = null;
        this.evaluatedAt = null;
    }

    public boolean isApproved() {
        return this.approvalStatus == SitterApprovalStatus.APPROVED;
    }

    public boolean isReservable() {
        return this.status == SitterProfileStatus.RESERVABLE;
    }
}
