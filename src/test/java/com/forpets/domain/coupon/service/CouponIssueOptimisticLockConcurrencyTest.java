package com.forpets.domain.coupon.service;

import com.forpets.domain.coupon.entity.Coupon;
import com.forpets.domain.coupon.exception.CouponErrorCode;
import com.forpets.domain.coupon.exception.CouponException;
import com.forpets.domain.coupon.repository.CouponRepository;
import com.forpets.domain.coupon.repository.UserCouponRepository;
import com.forpets.domain.coupon.service.issue.CouponIssueOptimisticLockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    private CouponIssueOptimisticLockService couponIssueOptimisticLockService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Test
    @DisplayName("낙관적 락 충돌 시 재시도하여 쿠폰 총 수량만큼 모두 발급한다")
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
        int requestCount = 1000;

        // 동시에 실행할 스레드 풀을 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        // 모든 스레드가 같은 시점에 시작하도록 대기
        CountDownLatch startLatch = new CountDownLatch(1);

        // 모든 요청이 끝날 때까지 테스트 대기
        CountDownLatch doneLatch = new CountDownLatch(requestCount);

        // 발급 성공 횟수 기록
        AtomicInteger successCount = new AtomicInteger();

        // 발급 실패 횟수 기록
        AtomicInteger failCount = new AtomicInteger();

        // 쿠폰 소진으로 실패한 요청 수 기록
        AtomicInteger soldOutFailCount = new AtomicInteger();

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

                    // 낙관적 락 충돌 재시도 후 쿠폰 소진으로 실패했는지 확인
                    if (isCouponException(e, CouponErrorCode.COUPON_QUANTITY_EXHAUSTED)) {
                        soldOutFailCount.incrementAndGet();
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
        System.out.println("쿠폰 소진 실패 수 = " + soldOutFailCount.get());
        System.out.println("쿠폰 총 수량 = " + foundCoupon.getTotalQuantity());
        System.out.println("쿠폰 잔여 수량 = " + foundCoupon.getRemainingQuantity());
        System.out.println("실제 UserCoupon 발급 개수 = " + issuedUserCouponCount);
        System.out.println("쿠폰 version = " + foundCoupon.getVersion());
        System.out.println("전체 처리 시간(ms) = " + (endTime - startTime));
        System.out.println("===============================================================");

        // 재시도 로직을 통해 실제 성공 수는 쿠폰 총 수량과 일치
        assertThat(successCount.get())
                .isEqualTo(foundCoupon.getTotalQuantity());

        // 전체 요청 중 쿠폰 총 수량을 제외한 요청은 실패
        assertThat(failCount.get())
                .isEqualTo(requestCount - foundCoupon.getTotalQuantity());

        // 실패 요청은 낙관적 락 충돌 자체가 아니라 최종 쿠폰 소진으로 실패
        assertThat(soldOutFailCount.get())
                .isEqualTo(requestCount - foundCoupon.getTotalQuantity());

        // 실제 발급 개수는 쿠폰 총 수량과 일치
        assertThat(issuedUserCouponCount)
                .isEqualTo(foundCoupon.getTotalQuantity());

        // 쿠폰이 모두 발급되었으므로 잔여 수량은 0이어야 한다.
        assertThat(foundCoupon.getRemainingQuantity())
                .isZero();

        // 쿠폰 수량만큼 차감되었으므로 version도 발급 성공 횟수만큼 증가
        assertThat(foundCoupon.getVersion())
                .isEqualTo((long) foundCoupon.getTotalQuantity());
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
}
