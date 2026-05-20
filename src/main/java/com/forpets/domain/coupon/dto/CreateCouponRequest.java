package com.forpets.domain.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateCouponRequest(

        @NotBlank(message = "쿠폰 이름은 필수입니다.")
        String name,

        // 현재 10% 단일 정책이므로 할인율은 서버에서 고정합니다.
        @Positive(message = "발급 수량은 1 이상이어야 합니다.")
        int totalQuantity
) {
}
