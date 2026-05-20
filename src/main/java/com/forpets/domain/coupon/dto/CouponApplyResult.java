package com.forpets.domain.coupon.dto;

// 결제 전용
public record CouponApplyResult(
        Long discountAmount,
        Long finalPrice,
        String couponName
) {
}
