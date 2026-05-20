package com.forpets.domain.coupon.dto;

import com.forpets.domain.coupon.entity.Coupon;
import com.forpets.domain.coupon.entity.UserCoupon;
import com.forpets.domain.coupon.entity.UserCouponStatus;

import java.time.LocalDateTime;

public record IssueCouponResponse(
        Long userCouponId,
        String couponName,
        int discountRate,
        UserCouponStatus status,
        LocalDateTime issuedAt
) {

    public static IssueCouponResponse of(UserCoupon userCoupon, Coupon coupon) {
        return new IssueCouponResponse(
                userCoupon.getId(),
                coupon.getName(),
                coupon.getDiscountRate(),
                userCoupon.getStatus(),
                userCoupon.getCreatedAt()
        );
    }
}
