package com.forpets.domain.payment.dto;

import com.forpets.domain.payment.entity.PaymentWebhookStatus;

public record PortOneWebhookResponse(
        Long webhookId,
        PaymentWebhookStatus status
) {
}
