package com.forpets.domain.reservation.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "reservation",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_reservation_care_request", columnNames = "care_request_id"),
                @UniqueConstraint(name = "uk_reservation_proposal", columnNames = "proposal_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "sitter_profile_id", nullable = false)
    private Long sitterProfileId;

    @Column(name = "care_request_id")
    private Long careRequestId;

    @Column(name = "proposal_id")
    private Long proposalId;

    @Column(nullable = false)
    private int totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status;

    @Column(columnDefinition = "TEXT")
    private String requestMemo;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private boolean guardianPaid;

    @Column(nullable = false)
    private boolean sitterPaid;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_category", length = 30)
    private CancelCategory cancelCategory;

    @Builder
    private Reservation(Long memberId, Long sitterProfileId, Long careRequestId,
                        Long proposalId, int totalPrice, String requestMemo) {
        this.memberId = memberId;
        this.sitterProfileId = sitterProfileId;
        this.careRequestId = careRequestId;
        this.proposalId = proposalId;
        this.totalPrice = totalPrice;
        this.requestMemo = requestMemo;
        this.status = ReservationStatus.PENDING;
        this.guardianPaid = false;
        this.sitterPaid = false;
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void complete() {
        this.status = ReservationStatus.COMPLETED;
    }

    public void cancel(String cancelReason, CancelCategory cancelCategory) {
        this.status = ReservationStatus.CANCELED;
        this.cancelReason = cancelReason;
        this.cancelCategory = cancelCategory;
    }

    public void expire() {
        this.status = ReservationStatus.EXPIRED;
    }

    public void confirmGuardianPayment() {
        this.guardianPaid = true;
    }

    public void confirmSitterPayment() {
        this.sitterPaid = true;
    }

    public boolean isFullyPaid() {
        return this.guardianPaid && this.sitterPaid;
    }
}
