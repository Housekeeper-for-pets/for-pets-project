package com.forpets.domain.coupon.controller;

import com.forpets.domain.coupon.dto.CouponLoadTestSummaryResponse;
import com.forpets.domain.coupon.dto.IssueCouponResponse;
import com.forpets.domain.coupon.service.issue.CouponIssueStrategy;
import com.forpets.domain.coupon.service.issue.CouponLoadTestService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/coupons")
@RequiredArgsConstructor
@Profile("loadtest")
public class CouponLoadTestController {

    private final CouponLoadTestService couponLoadTestService;

    // 락 없이 기존 쿠폰 발급
    @PostMapping("/{couponId}/issue/no-lock")
    @PreAuthorize("hasAnyRole('MEMBER', 'SITTER')")
    public ResponseEntity<ApiResponse<IssueCouponResponse>> issueWithNoLock(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long couponId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        couponLoadTestService.issue(currentMember.id(), couponId, CouponIssueStrategy.NO_LOCK)
                ));
    }

    // 비관적 락 쿠폰 발급
    @PostMapping("/{couponId}/issue/pessimistic")
    @PreAuthorize("hasAnyRole('MEMBER', 'SITTER')")
    public ResponseEntity<ApiResponse<IssueCouponResponse>> issueWithPessimisticLock(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long couponId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        couponLoadTestService.issue(currentMember.id(), couponId, CouponIssueStrategy.PESSIMISTIC)
                ));
    }

    // 낙관적 락 쿠폰 발급
    @PostMapping("/{couponId}/issue/optimistic")
    @PreAuthorize("hasAnyRole('MEMBER', 'SITTER')")
    public ResponseEntity<ApiResponse<IssueCouponResponse>> issueWithOptimisticLock(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long couponId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        couponLoadTestService.issue(currentMember.id(), couponId, CouponIssueStrategy.OPTIMISTIC)
                ));
    }

    // 분산 락 쿠폰 발급
    @PostMapping("/{couponId}/issue/distributed")
    @PreAuthorize("hasAnyRole('MEMBER', 'SITTER')")
    public ResponseEntity<ApiResponse<IssueCouponResponse>> issueWithDistributedLock(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long couponId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        couponLoadTestService.issue(currentMember.id(), couponId, CouponIssueStrategy.DISTRIBUTED)
                ));
    }

    // 테스트 이후 실제 발급 수량, 잔여 수량 확인
    @GetMapping("/{couponId}/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CouponLoadTestSummaryResponse>> getSummary(
            @PathVariable Long couponId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(couponLoadTestService.getSummary(couponId))
        );
    }
}