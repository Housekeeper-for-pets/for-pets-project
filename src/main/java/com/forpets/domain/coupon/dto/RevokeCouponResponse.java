package com.forpets.domain.coupon.dto;

import com.forpets.domain.coupon.entity.UserCoupon;
import com.forpets.domain.coupon.entity.UserCouponStatus;

public record RevokeCouponResponse(
        Long userCouponId,
        UserCouponStatus status
) {

    public static RevokeCouponResponse from(UserCoupon userCoupon) {
        return new RevokeCouponResponse(
                userCoupon.getId(),
                userCoupon.getStatus()
        );
    }
}
