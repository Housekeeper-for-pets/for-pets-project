package com.forpets.domain.proposal.dto;

import com.forpets.domain.proposal.entity.Proposal;
import com.forpets.domain.proposal.entity.ProposalStatus;

import java.time.LocalDateTime;

public record ProposalResponseDto(
        Long id,
        Long postId,
        Long sitterProfileId,
        Long sitterMemberId,
        Long memberId,
        Integer proposedPrice,
        String message,
        ProposalStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProposalResponseDto from(Proposal proposal) {
        return new ProposalResponseDto(
                proposal.getId(),
                proposal.getPostId(),
                proposal.getSitterProfileId(),
                proposal.getSitterMemberId(),
                proposal.getMemberId(),
                proposal.getProposedPrice(),
                proposal.getMessage(),
                proposal.getStatus(),
                proposal.getCreatedAt(),
                proposal.getUpdatedAt()
        );
    }
}