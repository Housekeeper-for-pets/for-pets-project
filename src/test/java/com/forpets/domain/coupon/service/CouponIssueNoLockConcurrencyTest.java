package com.forpets.domain.coupon.service;

import com.forpets.domain.coupon.entity.Coupon;
import com.forpets.domain.coupon.repository.CouponRepository;
import com.forpets.domain.coupon.repository.UserCouponRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Disabled("noLock 동시성 문제 재현용 테스트. 문서화 후 CI에서는 제외")
class CouponIssueNoLockConcurrencyTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Test
    @DisplayName("락 없이 동시에 쿠폰을 발급하면 정합성 문제가 발생할 수 있다")
    void issueCouponWithoutLockConcurrencyTest() throws InterruptedException {
        // 테스트용 쿠폰을 생성
        Coupon coupon = couponRepository.save(
                Coupon.builder()
                        .name("noLock 동시성 테스트 쿠폰")
                        .discountRate(10)
                        .totalQuantity(100)
                        .build()
        );

        // 동시에 쿠폰 발급을 요청할 사용자 수
        int requestCount = 300;

        // 동시에 실행할 스레드 풀을 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        // 모든 요청이 끝날 때까지 테스트 대기
        CountDownLatch doneLatch = new CountDownLatch(requestCount);

        // 발급 성공 횟수를 기록
        AtomicInteger successCount = new AtomicInteger();

        // 발급 실패 횟수를 기록
        AtomicInteger failCount = new AtomicInteger();

        long startTime = System.currentTimeMillis();

        // 여러 사용자가 같은 쿠폰을 동시에 발급받는 상황
        for (int i = 1; i <= requestCount; i++) {
            Long userId = (long) i;

            // 스레드 풀 작업 시작
            executorService.submit(() -> {
                try {
                    couponService.issueCoupon(userId, coupon.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 모든 스레드의 작업이 끝날 때까지 대기
        assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();

        // 스레드 풀 종료
        executorService.shutdown();

        long endTime = System.currentTimeMillis();

        // 테스트 종료 후 쿠폰 상태를 다시 조회
        Coupon foundCoupon = couponRepository.findById(coupon.getId()).orElseThrow();

        // 실제로 저장된 UserCoupon 개수를 조회
        long issuedUserCouponCount = userCouponRepository.countByCouponId(coupon.getId());

        System.out.println("========== NO LOCK 쿠폰 동시성 테스트 결과 ==========");
        System.out.println("총 요청 수 = " + requestCount);
        System.out.println("성공 수 = " + successCount.get());
        System.out.println("실패 수 = " + failCount.get());
        System.out.println("쿠폰 총 수량 = " + foundCoupon.getTotalQuantity());
        System.out.println("쿠폰 잔여 수량 = " + foundCoupon.getRemainingQuantity());
        System.out.println("실제 UserCoupon 발급 개수 = " + issuedUserCouponCount);
        System.out.println("기대 잔여 수량 = " + (foundCoupon.getTotalQuantity() - issuedUserCouponCount));
        System.out.println("전체 처리 시간(ms) = " + (endTime - startTime));
        System.out.println("=================================================");

        // UserCoupon 발급 개수는 쿠폰 총 수량 초과 방지
        assertThat(issuedUserCouponCount)
                .isLessThanOrEqualTo(foundCoupon.getTotalQuantity());

        // 잔여 수량은 음수 방지
        assertThat(foundCoupon.getRemainingQuantity())
                .isGreaterThanOrEqualTo(0);
    }
}
