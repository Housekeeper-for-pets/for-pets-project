package com.forpets.domain.coupon.dto;

import com.forpets.domain.coupon.entity.Coupon;

public record CouponLoadTestSummaryResponse(
        Long couponId,
        int totalQuantity,
        int remainingQuantity, // 남은 수량
        long issuedCount, // 실제 발급
        long expectedRemainingQuantity, // 총 수량 - 실제 발급 수
        boolean quantityNotExceeded, // 총 수량보다 많이 발급이냐?
        boolean remainingQuantityMatched, // 잔여 수량 확인
        boolean consistent // 최종 정합성 여부
) {
    public static CouponLoadTestSummaryResponse of(Coupon coupon, long issuedCount){
        long expectedRemainingQuantity = coupon.getTotalQuantity() - issuedCount;

        boolean quantityNotExceeded = issuedCount <= coupon.getTotalQuantity();
        boolean remainingQuantityMatched = coupon.getRemainingQuantity() == expectedRemainingQuantity;
        boolean consistent = quantityNotExceeded && remainingQuantityMatched && coupon.getRemainingQuantity()>=0;

        return new CouponLoadTestSummaryResponse(
                coupon.getId(),
                coupon.getTotalQuantity(),
                coupon.getRemainingQuantity(),
                issuedCount,
                expectedRemainingQuantity,
                quantityNotExceeded,
                remainingQuantityMatched,
                consistent
        );
    }
}
