package com.forpets.domain.coupon.entity;

public enum UserCouponStatus {
    ACTIVE,   // 발급됨, 사용 가능
    USED,     // 결제에 사용됨
    REVOKED   // 관리자가 회수함
}
