package com.forpets.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record FailPaymentRequest(
        @NotBlank(message = "결제 ID는 필수입니다")
        String merchantUid,

        @NotBlank(message = "결제 실패 사유는 필수입니다")
        String failedReason
) {}
