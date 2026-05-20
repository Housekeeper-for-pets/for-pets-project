package com.forpets.domain.proposal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateProposalRequest(
        @NotNull(message = "제안 금액은 필수입니다")
        @Positive(message = "제안 금액은 0보다 커야 합니다")
        Integer proposedPrice,

        String message
) {}