package com.forpets.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record ConfirmPaymentRequest(
        @NotBlank(message = "결제 ID는 필수입니다")
        @JsonAlias({"paymentId", "portonePaymentId"})
        String merchantUid
) {}
