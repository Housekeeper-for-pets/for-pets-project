package com.forpets.domain.coupon.service;

import com.forpets.domain.coupon.entity.Coupon;
import com.forpets.domain.coupon.exception.CouponErrorCode;
import com.forpets.domain.coupon.exception.CouponException;
import com.forpets.domain.coupon.repository.CouponRepository;
import com.forpets.domain.coupon.repository.UserCouponRepository;
import com.forpets.domain.coupon.service.issue.CouponIssueOptimisticLockService;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
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
@Disabled("Coupon @Version 비활성화 상태에서는 낙관적 락 테스트 제외")
class CouponIssueOptimisticLockConcurrencyTest {

    @Autowired
    private CouponIssueOptimisticLockService couponIssueOptimisticLockService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Test
    @DisplayName("낙관적 락 충돌 시 재시도하고 실패 원인을 분류한다")
    void issueCouponWithOptimisticLockRetryConcurrencyTest() throws InterruptedException {
        // 테스트용 쿠폰 생성
        Coupon coupon = couponRepository.save(
                Coupon.builder()
                        .name("optimisticLock retry concurrency test coupon")
                        .discountRate(10)
                        .totalQuantity(100)
                        .build()
        );

        // 동시에 쿠폰 발급을 요청할 사용자 수
        int requestCount = 300;

        // 동시에 실행할 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        // 모든 스레드가 같은 시점에 시작하도록 대기
        CountDownLatch startLatch = new CountDownLatch(1);

        // 모든 요청이 끝날 때까지 테스트 스레드가 기다리기 위한 latch
        CountDownLatch doneLatch = new CountDownLatch(requestCount);

        // 발급 성공 횟수 기록
        AtomicInteger successCount = new AtomicInteger();

        // 전체 발급 실패 횟수 기록
        AtomicInteger failCount = new AtomicInteger();

        // 쿠폰 소진으로 실패한 요청 수 기록
        AtomicInteger soldOutFailCount = new AtomicInteger();

        // 낙관적 락 재시도 횟수를 모두 소진해 실패한 요청 수 기록
        AtomicInteger optimisticRetryFailCount = new AtomicInteger();

        // 쿠폰 소진 / 낙관적 락 재시도 실패가 아닌 기타 실패 요청 수 기록
        AtomicInteger otherFailCount = new AtomicInteger();

        // 이전 테스트 실행 결과가 섞이지 않도록 실제 재시도 횟수 초기화
        couponIssueOptimisticLockService.resetActualRetryCount();

        long startTime = System.currentTimeMillis();

        // 여러 사용자가 같은 쿠폰을 동시에 발급받는 상황 생성
        for (int i = 1; i <= requestCount; i++) {
            Long userId = (long) i;

            executorService.submit(() -> {
                try {
                    // startLatch가 열릴 때까지 모든 스레드 대기
                    startLatch.await();

                    // @Version 기반 낙관적 락 충돌이 발생하면 서비스 내부에서 재시도
                    couponIssueOptimisticLockService.issue(userId, coupon.getId());

                    // 예외 없이 발급되면 성공 수 증가
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 재시도 이후에도 실패한 요청 수 기록
                    failCount.incrementAndGet();

                    // 실패 원인별 카운트 기록
                    if (isCouponException(e, CouponErrorCode.COUPON_QUANTITY_EXHAUSTED)) {
                        soldOutFailCount.incrementAndGet();
                    } else if (isOptimisticLockException(e)) {
                        optimisticRetryFailCount.incrementAndGet();
                    } else {
                        otherFailCount.incrementAndGet();
                    }
                } finally {
                    // 성공 / 실패와 관계없이 작업 완료 알림
                    doneLatch.countDown();
                }
            });
        }

        // 대기 중인 스레드들을 동시에 시작
        startLatch.countDown();

        // 모든 요청이 끝날 때까지 대기
        assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();

        // 테스트 종료 후 스레드 풀 종료
        executorService.shutdown();

        long endTime = System.currentTimeMillis();

        // 테스트 종료 후 쿠폰 상태 재조회
        Coupon foundCoupon = couponRepository.findById(coupon.getId()).orElseThrow();

        // 실제 저장된 UserCoupon 개수 조회
        long issuedUserCouponCount = userCouponRepository.countByCouponId(coupon.getId());

        System.out.println("========== OPTIMISTIC LOCK RETRY 쿠폰 동시성 테스트 결과 ==========");
        System.out.println("총 요청 수 = " + requestCount);
        System.out.println("성공 수 = " + successCount.get());
        System.out.println("실패 수 = " + failCount.get());
        System.out.println("  |- 쿠폰 소진 실패 = " + soldOutFailCount.get());
        System.out.println("  |- 낙관락 재시도 실패 = " + optimisticRetryFailCount.get());
        System.out.println("  |- 기타 실패 = " + otherFailCount.get());
        System.out.println("쿠폰 총 수량 = " + foundCoupon.getTotalQuantity());
        System.out.println("쿠폰 잔여 수량 = " + foundCoupon.getRemainingQuantity());
        System.out.println("실제 UserCoupon 발급 개수 = " + issuedUserCouponCount);
//        System.out.println("쿠폰 version = " + foundCoupon.getVersion());
        System.out.println("낙관적 락 최대 재시도 횟수 = " + couponIssueOptimisticLockService.getMaxRetryCount());
        System.out.println("낙관적 락 재시도 대기 시간(ms) = " + couponIssueOptimisticLockService.getRetryBackoffMillis());
        System.out.println("전체 처리 시간(ms) = " + (endTime - startTime));
        System.out.println("===============================================================");

        // 성공 수와 실제 발급 개수는 일치해야 한다.
        assertThat(successCount.get())
                .isEqualTo((int) issuedUserCouponCount);

        // 실패 원인별 카운트 합은 전체 실패 수와 일치해야 한다.
        assertThat(soldOutFailCount.get() + optimisticRetryFailCount.get() + otherFailCount.get())
                .isEqualTo(failCount.get());

        // 실제 발급 개수는 쿠폰 총 수량을 초과하면 안 된다.
        assertThat(issuedUserCouponCount)
                .isLessThanOrEqualTo(foundCoupon.getTotalQuantity());

        // 잔여 수량은 음수가 되면 안 된다.
        assertThat(foundCoupon.getRemainingQuantity())
                .isGreaterThanOrEqualTo(0);

        // 실제 발급 개수와 쿠폰 잔여 수량 계산 결과는 일치해야 한다.
        assertThat(foundCoupon.getRemainingQuantity())
                .isEqualTo(foundCoupon.getTotalQuantity() - issuedUserCouponCount);
    }

    // CouponException은 여러 예외로 감싸질 수 있어 원인 예외까지 확인
    private boolean isCouponException(Throwable throwable, CouponErrorCode errorCode) {
        while (throwable != null) {
            if (throwable instanceof CouponException couponException
                    && couponException.getErrorCode() == errorCode) {
                return true;
            }

            throwable = throwable.getCause();
        }

        return false;
    }

    // 낙관적 락 예외가 여러 예외로 감싸질 수 있어 원인 예외까지 확인
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
