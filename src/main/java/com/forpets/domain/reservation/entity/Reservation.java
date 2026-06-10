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

    /*
    UPDATE 시 JPA 가 자동으로 version 을 +1 하고, WHERE 절에 기존 version 을 추가
    동시에 두 트랜잭션이 같은 reservation 을 수정하면 한쪽은 OptimisticLockException 으로 실패
    Reservation Lock 으로 1차 직렬화 + @Version 으로 마지막 방어선
     */
    @Version
    @Column(nullable = false)
    private Long version;

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
        if (!isCancelable()) throw new ReservationException(ReservationErrorCode.INVALID_RESERVATION_STATUS_TRANSITION);
        this.status = ReservationStatus.CANCELED;
        this.cancelReason = cancelReason;
        this.cancelCategory = cancelCategory;
        this.canceledBy = canceledBy;
        this.canceledAt = LocalDateTime.now();
    }

    // 불가피한 이유로 취소 신청
    public void requestCancel(String cancelReason, CancelCategory cancelCategory, CanceledBy canceledBy) {
        if (this.status != ReservationStatus.CONFIRMED) {
            throw new ReservationException(ReservationErrorCode.INVALID_RESERVATION_STATUS_TRANSITION);
        }
        this.status = ReservationStatus.CANCEL_REQUESTED;
        this.cancelReason = cancelReason;
        this.cancelCategory = cancelCategory;
        this.canceledBy = canceledBy;
    }

    // 불가피한 이유로 취소 신청을 했지만 받아들여지지 않은 경우 다시 CONFIRMED 상태로 돌림
    public void restoreToConfirmed() {
        if (this.status != ReservationStatus.CANCEL_REQUESTED) {
            throw new ReservationException(ReservationErrorCode.INVALID_RESERVATION_STATUS_TRANSITION);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.cancelReason = null;
        this.cancelCategory = null;
        this.canceledBy = null;
    }

    public boolean isCancelRequested() {
        return this.status == ReservationStatus.CANCEL_REQUESTED;
    }

    public void expire() {
        if (!isPending()) return;
        this.status = ReservationStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return this.status == ReservationStatus.PENDING;
    }

    public boolean isConfirmed() {
        return this.status == ReservationStatus.CONFIRMED;
    }

    public boolean isExpired() {
        return this.status == ReservationStatus.EXPIRED;
    }

    public boolean isCanceled() {
        return this.status == ReservationStatus.CANCELED;
    }

    public boolean isCompleted() {
        return this.status == ReservationStatus.COMPLETED;
    }

    /*
    결제 가능 여부 — PENDING 일 때만 결제 시도/재시도 가능
    프론트의 결제 버튼 노출 조건과 1:1 대응되는 단일 진실(single source of truth)
     */
    public boolean isPayable() {
        return this.status == ReservationStatus.PENDING;
    }

    public boolean isCancelable() {
        return isPending() || isConfirmed() || isCancelRequested();
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
