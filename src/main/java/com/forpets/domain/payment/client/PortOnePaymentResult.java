package com.forpets.domain.payment.client;

public record PortOnePaymentResult(
        String paymentId,
        String status,
        Long totalAmount,
        String rawResponse
) {
    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(status) || "paid".equalsIgnoreCase(status);
    }
}
