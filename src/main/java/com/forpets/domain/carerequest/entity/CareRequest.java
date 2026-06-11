package com.forpets.domain.carerequest.entity;

import com.forpets.domain.proposal.entity.ProposalStatus;
import com.forpets.global.common.CareType;
import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "care_request")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "sitter_profile_id", nullable = false)
    private Long sitterProfileId;

    @Column(name = "sitter_member_id", nullable = false)
    private Long sitterMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareType careType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private int requestPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareRequestStatus status;

    @Builder
    private CareRequest(Long memberId, Long sitterProfileId, Long sitterMemberId,
                        CareType careType, String message, int requestPrice) {
        this.memberId = memberId;
        this.sitterProfileId = sitterProfileId;
        this.sitterMemberId = sitterMemberId;
        this.careType = careType;
        this.message = message;
        this.status = CareRequestStatus.PENDING;
        this.requestPrice = requestPrice;
    }

    public void accept() {
        this.status = CareRequestStatus.ACCEPTED;
    }

    public void reject() {
        this.status = CareRequestStatus.REJECTED;
    }

    public void cancel() {
        this.status = CareRequestStatus.CANCELED;
    }

    public void expire() {
        if (status != CareRequestStatus.PENDING && status != CareRequestStatus.ACCEPTED) return;
        this.status = CareRequestStatus.EXPIRED;
    }

    public boolean isPending() {
        return this.status == CareRequestStatus.PENDING;
    }

    public boolean isAccepted() {
        return this.status == CareRequestStatus.ACCEPTED;
    }

    public boolean isExpired() {
        return this.status == CareRequestStatus.EXPIRED;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public boolean isTargetSitter(Long sitterProfileId) {
        return this.sitterProfileId.equals(sitterProfileId);
    }

    public void restoreToPending() {
        // ACCEPTED 만 PENDING 으로 복원 — 그 외 상태(EXPIRED 등)에서의 호출은 무시
        if (status != CareRequestStatus.ACCEPTED) return;
        this.status = CareRequestStatus.PENDING;
    }
}