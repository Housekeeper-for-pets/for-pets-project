package com.forpets.domain.coupon.controller;

import com.forpets.domain.coupon.dto.IssueCouponResponse;
import com.forpets.domain.coupon.service.issue.CouponIssueDistributedLockService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponIssueDistributedLockService couponIssueDistributedLockService;

    // 로그인한 회원 쿠폰을 발급
    @PostMapping("/{couponId}/issue")
    @PreAuthorize("hasAnyRole('MEMBER', 'SITTER')")
    public ResponseEntity<ApiResponse<IssueCouponResponse>> issueCoupon(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long couponId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        couponIssueDistributedLockService.issue(currentMember.id(), couponId)
                ));
    }
}
