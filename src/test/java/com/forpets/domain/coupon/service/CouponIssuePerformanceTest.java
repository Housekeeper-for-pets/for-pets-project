package com.forpets.domain.coupon.service;

import com.forpets.domain.coupon.entity.Coupon;
import com.forpets.domain.coupon.repository.CouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
@DisplayName("쿠폰 대기 시간 설정")
class CouponIssuePerformanceTest {

    // 측정 전 테스트 환경을 안정화하기 위한 워밍업 횟수
    private static final int WARM_UP_COUNT = 50;

    // 쿠폰 발급 처리 시간을 측정할 반복 횟수
    private static final int MEASURE_COUNT = 1000;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Test
    @DisplayName("쿠폰 발급 처리 시간 p99 기준으로 재시도 대기 시간 후보를 산출한다")
    void calculateRetryBackoffCandidateFromCouponIssueProcessingTime() {
        // 실제 측정 전 쿠폰 발급 로직을 미리 실행
        warmUpCouponIssue();

        // 처리 시간 측정용 쿠폰 생성
        Coupon coupon = couponRepository.save(
                Coupon.builder()
                        .name("coupon issue processing time measurement coupon")
                        .discountRate(10)
                        .totalQuantity(MEASURE_COUNT)
                        .build()
        );

        // 각 발급 요청의 처리 시간을 기록
        long[] elapsedNanos = new long[MEASURE_COUNT];

        // 쿠폰 발급 1회 처리 시간을 반복 측정
        for (int i = 0; i < MEASURE_COUNT; i++) {
            Long userId = (long) (i + 1);

            long start = System.nanoTime();

            couponService.issueCoupon(userId, coupon.getId());

            long end = System.nanoTime();
            elapsedNanos[i] = end - start;
        }

        // p99 계산을 위해 측정 시간을 정렬
        Arrays.sort(elapsedNanos);

        // 처리 시간 통계 계산
        double averageMillis = averageMillis(elapsedNanos);
        double p99Millis = percentileMillis(elapsedNanos, 99);

        // 낙관적 락 재시도 대기 시간 후보를 p99 기준으로 산출
        long retryBackoffCandidateMillis = (long) Math.ceil(p99Millis);

        System.out.println("========== 쿠폰 발급 처리 시간 측정 결과 ==========");
        System.out.println("워밍업 횟수 = " + WARM_UP_COUNT);
        System.out.println("측정 횟수 = " + MEASURE_COUNT);
        System.out.println("평균 처리 시간(ms) = " + averageMillis);
        System.out.println("p99 처리 시간(ms) = " + p99Millis);
        System.out.println("재시도 대기 시간 후보(ms) = " + retryBackoffCandidateMillis);
        System.out.println("===============================================");
    }

    private void warmUpCouponIssue() {
        // 워밍업용 쿠폰 생성
        Coupon coupon = couponRepository.save(
                Coupon.builder()
                        .name("coupon issue processing time warm-up coupon")
                        .discountRate(10)
                        .totalQuantity(WARM_UP_COUNT)
                        .build()
        );

        // 초기 JPA 준비 비용이 실제 측정에 섞이지 않도록 미리 실행
        for (int i = 0; i < WARM_UP_COUNT; i++) {
            Long userId = (long) -(i + 1);
            couponService.issueCoupon(userId, coupon.getId());
        }
    }

    private double averageMillis(long[] elapsedNanos) {
        long totalElapsedNanos = 0L;

        for (long elapsedNano : elapsedNanos) {
            totalElapsedNanos += elapsedNano;
        }

        return toMillis(totalElapsedNanos) / elapsedNanos.length;
    }

    private double percentileMillis(long[] sortedElapsedNanos, int percentile) {
        int index = (int) Math.ceil(sortedElapsedNanos.length * percentile / 100.0) - 1;
        return toMillis(sortedElapsedNanos[index]);
    }

    private double toMillis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
