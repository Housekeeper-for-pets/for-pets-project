package com.forpets.domain.coupon.controller;

import com.forpets.domain.coupon.dto.CouponResponse;
import com.forpets.domain.coupon.dto.CreateCouponRequest;
import com.forpets.domain.coupon.dto.RevokeCouponResponse;
import com.forpets.domain.coupon.service.CouponService;
import com.forpets.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CouponAdminController {

    private final CouponService couponService;

    // 관리자 쿠폰 생성
    @PostMapping("/coupons")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
            @RequestBody @Valid CreateCouponRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(couponService.createCoupon(request)));
    }

    // 유저 쿠폰 회수
    @PatchMapping("/user-coupons/{userCouponId}/revoke")
    public ResponseEntity<ApiResponse<RevokeCouponResponse>> revokeCoupon(
            @PathVariable Long userCouponId
    ) {
        return ResponseEntity.ok(ApiResponse.success(couponService.revokeCoupon(userCouponId)));
    }
}
