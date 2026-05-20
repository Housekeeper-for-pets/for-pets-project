package com.forpets.domain.coupon.dto;

public record CouponResponse(
        Long couponId,
        String name,
        int discountRate,
        int totalQuantity,
        int remainingQuantity
) {

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getDiscountRate(),
                coupon.getTotalQuantity(),
                coupon.getRemainingQuantity()
        );
    }
}
