package com.forpets.domain.coupon.service;

import com.forpets.domain.coupon.entity.Coupon;
import com.forpets.domain.coupon.repository.CouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("쿠폰 대기 시간 설정")
class CouponIssuePerformanceTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Test
    void measureCouponIssuePureProcessingTime() {
        Coupon coupon = couponRepository.save(
                Coupon.builder()
                        .name("single thread performance test coupon")
                        .discountRate(10)
                        .totalQuantity(10)
                        .build()
        );

        int repeatCount = 10;
        long totalElapsedNanos = 0L;

        for (int i = 1; i <= repeatCount; i++) {
            Long userId = (long) i;

            long start = System.nanoTime();

            couponService.issueCoupon(userId, coupon.getId());

            long end = System.nanoTime();
            long elapsedNanos = end - start;
            totalElapsedNanos += elapsedNanos;

            System.out.println(i + "회차 처리 시간(ms) = " + elapsedNanos / 1_000_000.0);
        }

        double averageMillis = totalElapsedNanos / 1_000_000.0 / repeatCount;

        System.out.println("평균 처리 시간(ms) = " + averageMillis);
        System.out.println("재시도 대기 시간 후보(ms) = " + Math.ceil(averageMillis));
    }
}