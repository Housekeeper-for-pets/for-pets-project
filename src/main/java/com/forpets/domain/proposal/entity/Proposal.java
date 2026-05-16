package com.forpets.domain.proposal.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "proposal", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"post_id", "sitter_profile_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Proposal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "sitter_profile_id", nullable = false)
    private Long sitterProfileId;

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
    private Proposal(Long postId, Long sitterProfileId, Long memberId,
                     Integer proposedPrice, String message) {
        this.postId = postId;
        this.sitterProfileId = sitterProfileId;
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

    public void withdraw(String reason) {
        this.status = ProposalStatus.WITHDRAWN;
    }

    public boolean isPending() {
        return this.status == ProposalStatus.PENDING;
    }

    public boolean isOwnedBySitter(Long sitterProfileId) {
        return this.sitterProfileId.equals(sitterProfileId);
    }
}