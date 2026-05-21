package com.forpets.domain.payment.dto;

import com.forpets.domain.payment.entity.Payment;
import com.forpets.domain.payment.entity.PaymentProvider;
import com.forpets.domain.payment.entity.PaymentStatus;

public record PaymentResponseDto(
        Long paymentId,
        Long reservationId,
        String merchantUid,
        Long originalAmount,
        Long discountAmount,
        Long finalAmount,
        PaymentProvider provider,
        PaymentStatus status
) {
    public static PaymentResponseDto from(Payment payment) {
        return new PaymentResponseDto(
                payment.getId(),
                payment.getReservationId(),
                payment.getMerchantUid(),
                payment.getOriginalAmount(),
                payment.getDiscountAmount(),
                payment.getFinalAmount(),
                payment.getProvider(),
                payment.getStatus()
        );
    }
}
