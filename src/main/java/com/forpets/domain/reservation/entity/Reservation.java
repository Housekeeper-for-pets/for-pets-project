package com.forpets.domain.reservation.entity;

import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import com.forpets.global.common.CareType;
import com.forpets.global.entity.BaseEntity;
import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.CommonErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "reservation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long guardianId;

    @Column(name = "sitter_member_id", nullable = false)
    private Long sitterMemberId;

    @Column(name = "sitter_profile_id", nullable = false)
    private Long sitterProfileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareType careType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationSource source;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(length = 500)
    private String cancelReason;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CancelCategory cancelCategory;

    @Column
    private LocalDateTime canceledAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CanceledBy canceledBy;

    @Column
    private LocalDateTime confirmedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime expiredAt;

    @Builder
    private Reservation(Long guardianId, Long sitterMemberId, Long sitterProfileId,
                        CareType careType, ReservationSource source, Long sourceId) {
        this.guardianId = guardianId;
        this.sitterMemberId = sitterMemberId;
        this.sitterProfileId = sitterProfileId;
        this.careType = careType;
        this.status = ReservationStatus.PENDING;
        this.source = source;
        this.sourceId = sourceId;
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void complete() {
        if (this.status != ReservationStatus.CONFIRMED) throw new ReservationException(ReservationErrorCode.INVALID_RESERVATION_STATUS_TRANSITION);
        this.status = ReservationStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel(String cancelReason, CancelCategory cancelCategory, CanceledBy canceledBy) {
        this.status = ReservationStatus.CANCELED;
        this.cancelReason = cancelReason;
        this.cancelCategory = cancelCategory;
        this.canceledBy = canceledBy;
        this.canceledAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = ReservationStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return this.status == ReservationStatus.PENDING;
    }

    public boolean isConfirmed() {
        return this.status == ReservationStatus.CONFIRMED;
    }

    public boolean isCancelable() {
        return isPending() || isConfirmed();
    }

    public boolean isParty(Long memberId) {
        return this.guardianId.equals(memberId) || this.sitterMemberId.equals(memberId);
    }

    public boolean isSitter(Long memberId) {
        return this.sitterMemberId.equals(memberId);
    }

    public boolean isGuardian(Long memberId) {
        return this.guardianId.equals(memberId);
    }
}
