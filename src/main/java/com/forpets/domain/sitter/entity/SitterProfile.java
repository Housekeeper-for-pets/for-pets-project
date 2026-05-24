package com.forpets.domain.sitter.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

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
        this.status = SitterProfileStatus.RESERVABLE;
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
