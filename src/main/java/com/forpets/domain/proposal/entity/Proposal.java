package com.forpets.domain.proposal.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "proposal")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Proposal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "sitter_profile_id", nullable = false)
    private Long sitterProfileId;

    @Column(name = "sitter_member_id", nullable = false)
    private Long sitterMemberId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Integer proposedPrice;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProposalStatus status;

    @Builder
    private Proposal(Long postId, Long sitterProfileId, Long memberId, Long sitterMemberId,
                     Integer proposedPrice, String message) {
        this.postId = postId;
        this.sitterProfileId = sitterProfileId;
        this.sitterMemberId = sitterMemberId;
        this.memberId = memberId;
        this.proposedPrice = proposedPrice;
        this.message = message;
        this.status = ProposalStatus.PENDING;
    }

    public void accept() {
        this.status = ProposalStatus.ACCEPTED;
    }

    public void reject() {
        this.status = ProposalStatus.REJECTED;
    }

    public void withdraw() {
        this.status = ProposalStatus.WITHDRAWN;
    }

    public void expire() {
        if (status != ProposalStatus.PENDING && status != ProposalStatus.ACCEPTED) return;
        this.status = ProposalStatus.EXPIRED;
    }

    public void restoreToPending() {
        // ACCEPTED 만 PENDING 으로 복원 — 그 외 상태(EXPIRED 등)에서의 호출은 무시
        if (status != ProposalStatus.ACCEPTED) return;
        this.status = ProposalStatus.PENDING;
    }

    public boolean isPending() {
        return this.status == ProposalStatus.PENDING;
    }

    public boolean isExpired() {
        return this.status == ProposalStatus.EXPIRED;
    }

    public boolean isOwnedBySitter(Long sitterProfileId) {
        return this.sitterProfileId.equals(sitterProfileId);
    }
}