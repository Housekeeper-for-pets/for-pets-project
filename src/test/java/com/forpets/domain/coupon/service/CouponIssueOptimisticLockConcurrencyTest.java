package com.forpets.domain.coupon.service;

import com.forpets.domain.coupon.entity.Coupon;
import com.forpets.domain.coupon.repository.CouponRepository;
import com.forpets.domain.coupon.repository.UserCouponRepository;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.redisson.api.RedissonClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponIssueOptimisticLockConcurrencyTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @MockitoBean
    private RedissonClient redissonClient;

    @Test
    @DisplayName("낙관적 락을 적용하면 동시에 쿠폰을 발급해도 총 수량을 초과하지 않는다")
    void issueCouponWithOptimisticLockConcurrencyTest() throws InterruptedException {
        // 테스트용 쿠폰을 생성
        Coupon coupon = couponRepository.save(
                Coupon.builder()
                        .name("optimisticLock 동시성 테스트 쿠폰")
                        .discountRate(10)
                        .totalQuantity(100)
                        .build()
        );

        // 동시에 쿠폰 발급을 요청할 사용자 수
        int requestCount = 1000;

        // 동시에 실행할 스레드 풀을 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        // 모든 스레드가 같은 시점에 시작하도록 대기
        CountDownLatch startLatch = new CountDownLatch(1);

        // 모든 요청이 끝날 때까지 테스트 대기
        CountDownLatch doneLatch = new CountDownLatch(requestCount);

        // 발급 성공 횟수 기록
        AtomicInteger successCount = new AtomicInteger();

        // 전체 실패 횟수 기록
        AtomicInteger failCount = new AtomicInteger();

        // 낙관적 락 충돌 실패 횟수 기록
        AtomicInteger optimisticLockFailCount = new AtomicInteger();

        long startTime = System.currentTimeMillis();

        // 여러 사용자가 같은 쿠폰을 동시에 발급받는 상황 생성
        for (int i = 1; i <= requestCount; i++) {
            Long userId = (long) i;

            executorService.submit(() -> {
                try {
                    // startLatch가 열릴 때까지 모든 스레드 대기
                    startLatch.await();

                    // Coupon의 @Version으로 동시 수정 시 버전 충돌 감지
                    couponService.issueCoupon(userId, coupon.getId());

                    // 예외 없이 커밋까지 성공하면 성공으로 기록
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 실패한 요청 수 기록
                    failCount.incrementAndGet();

                    // 실패 원인이 낙관적 락 충돌인지 확인
                    if (isOptimisticLockException(e)) {
                        optimisticLockFailCount.incrementAndGet();
                    }
                } finally {
                    // 성공하든 실패하든 작업 완료 알림
                    doneLatch.countDown();
                }
            });
        }

        // 대기 중인 스레드들을 동시에 시작
        startLatch.countDown();

        // 모든 스레드의 작업이 끝날 때까지 대기
        assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();

        // 테스트 종료 후 스레드 풀 종료
        executorService.shutdown();

        long endTime = System.currentTimeMillis();

        // 테스트 종료 후 쿠폰 상태 재조회
        Coupon foundCoupon = couponRepository.findById(coupon.getId()).orElseThrow();

        // 실제 저장된 UserCoupon 개수 조회
        long issuedUserCouponCount = userCouponRepository.countByCouponId(coupon.getId());

        System.out.println("========== OPTIMISTIC LOCK 쿠폰 동시성 테스트 결과 ==========");
        System.out.println("총 요청 수 = " + requestCount);
        System.out.println("성공 수 = " + successCount.get());
        System.out.println("실패 수 = " + failCount.get());
        System.out.println("낙관적 락 충돌 실패 수 = " + optimisticLockFailCount.get());
        System.out.println("쿠폰 총 수량 = " + foundCoupon.getTotalQuantity());
        System.out.println("쿠폰 잔여 수량 = " + foundCoupon.getRemainingQuantity());
        System.out.println("실제 UserCoupon 발급 개수 = " + issuedUserCouponCount);
        System.out.println("기대 잔여 수량 = " + (foundCoupon.getTotalQuantity() - issuedUserCouponCount));
        System.out.println("전체 처리 시간(ms) = " + (endTime - startTime));
        System.out.println("=========================================================");

        // 낙관적 락 적용 후 실제 발급 개수는 쿠폰 총 수량 초과 방지
        assertThat(issuedUserCouponCount)
                .isLessThanOrEqualTo(foundCoupon.getTotalQuantity());

        // 잔여 수량은 음수 방지
        assertThat(foundCoupon.getRemainingQuantity())
                .isGreaterThanOrEqualTo(0);

        // 실제 발급 개수와 쿠폰 잔여 수량 계산 결과 일치
        assertThat(foundCoupon.getRemainingQuantity())
                .isEqualTo(foundCoupon.getTotalQuantity() - issuedUserCouponCount);
    }

    // 낙관적 락 예외는 여러 타입으로 감싸질 수 있어 원인 예외까지 확인
    private boolean isOptimisticLockException(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof OptimisticLockException
                    || throwable instanceof OptimisticLockingFailureException
                    || throwable instanceof StaleObjectStateException) {
                return true;
            }

            throwable = throwable.getCause();
        }

        return false;
    }
}
