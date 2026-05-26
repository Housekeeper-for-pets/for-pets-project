package com.forpets.domain.payment.dto;

import com.forpets.domain.payment.entity.Payment;
import com.forpets.domain.payment.entity.PaymentProvider;
import com.forpets.domain.payment.entity.PaymentRole;
import com.forpets.domain.payment.entity.PaymentStatus;
import com.forpets.domain.payment.entity.PaymentType;

import java.time.LocalDateTime;

public record PaymentResponseDto(
        Long paymentId,
        Long reservationId,
        Long memberId,
        PaymentRole paymentRole,
        PaymentType paymentType,
        String merchantUid,
        Long originalAmount,
        Long discountAmount,
        Long finalAmount,
        Long userCouponId,
        PaymentProvider provider,
        PaymentStatus status,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        LocalDateTime canceledAt,
        LocalDateTime refundedAt
) {
    public static PaymentResponseDto from(Payment payment) {
        return new PaymentResponseDto(
                payment.getId(),
                payment.getReservationId(),
                payment.getMemberId(),
                payment.getPaymentRole(),
                payment.getPaymentType(),
                payment.getMerchantUid(),
                payment.getOriginalAmount(),
                payment.getDiscountAmount(),
                payment.getFinalAmount(),
                payment.getUserCouponId(),
                payment.getProvider(),
                payment.getStatus(),
                payment.getRequestedAt(),
                payment.getApprovedAt(),
                payment.getCanceledAt(),
                payment.getRefundedAt()
        );
    }
}
