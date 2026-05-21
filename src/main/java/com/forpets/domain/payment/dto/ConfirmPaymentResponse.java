package com.forpets.domain.payment.dto;

import com.forpets.domain.payment.entity.Payment;
import com.forpets.domain.payment.entity.PaymentStatus;
import com.forpets.domain.reservation.entity.ReservationStatus;

public record ConfirmPaymentResponse(
        Long paymentId,
        PaymentStatus status,
        ReservationStatus reservationStatus
) {
    public static ConfirmPaymentResponse of(Payment payment, ReservationStatus reservationStatus) {
        return new ConfirmPaymentResponse(
                payment.getId(),
                payment.getStatus(),
                reservationStatus
        );
    }
}
