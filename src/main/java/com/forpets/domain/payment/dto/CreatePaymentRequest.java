package com.forpets.domain.payment.dto;

import com.forpets.domain.payment.entity.PaymentRole;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull(message = "예약 ID는 필수입니다")
        Long reservationId,

        @NotNull(message = "결제 역할은 필수입니다")
        PaymentRole paymentRole
) {}
