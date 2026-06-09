package com.forpets.domain.coupon.service;

import com.forpets.domain.coupon.entity.Coupon;
import com.forpets.domain.coupon.exception.CouponErrorCode;
import com.forpets.domain.coupon.exception.CouponException;
import com.forpets.domain.coupon.repository.CouponRepository;
import com.forpets.domain.coupon.repository.UserCouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class CouponIssuePessimisticLockConcurrencyTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @MockitoBean
    private RedissonClient redissonClient;

    @ParameterizedTest
    @ValueSource(ints = {300, 500, 700, 1000, 2000})
    @DisplayName("비관적 락 요청 수 증가 테스트")
    void issueCouponWithPessimisticLockTrafficIncreaseTest(int requestCount) throws InterruptedException {
        // 테스트용 쿠폰 생성
        Coupon coupon = couponRepository.save(
                Coupon.builder()
                        .name("pessimisticLock 요청 수 증가 테스트 쿠폰")
                        .discountRate(10)
                        .totalQuantity(100)
                        .build()
        );

        // application 테스트에서는 스레드 풀 크기를 고정하고 총 요청 수만 증가시킨다.
        int threadPoolSize = 32;

        // 동시에 실행할 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        // 모든 스레드가 같은 시점에 시작하도록 대기
        CountDownLatch startLatch = new CountDownLatch(1);

        // 모든 요청이 끝날 때까지 테스트 대기
        CountDownLatch doneLatch = new CountDownLatch(requestCount);

        // 발급 성공 횟수 기록
        AtomicInteger successCount = new AtomicInteger();

        // 전체 실패 횟수 기록
        AtomicInteger failCount = new AtomicInteger();

        // 쿠폰 소진 실패 횟수 기록
        AtomicInteger soldOutFailCount = new AtomicInteger();

        // 쿠폰 소진 외 예상하지 못한 실패 횟수 기록
        AtomicInteger otherFailCount = new AtomicInteger();

        // 여러 사용자가 같은 쿠폰을 동시에 발급받는 상황 생성
        for (int i = 1; i <= requestCount; i++) {
            Long userId = (long) i;

            executorService.submit(() -> {
                try {
                    // startLatch가 열릴 때까지 모든 스레드 대기
                    startLatch.await();

                    // 비관적 락이 적용된 발급 메서드 호출
                    couponService.issueCouponWithPessimisticLock(userId, coupon.getId());

                    // 예외 없이 발급되면 성공 수 증가
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 예외가 발생하면 실패 수 증가
                    failCount.incrementAndGet();

                    // 쿠폰 소진 실패와 기타 실패를 구분
                    if (isSoldOutException(e)) {
                        soldOutFailCount.incrementAndGet();
                    } else {
                        otherFailCount.incrementAndGet();
                    }
                } finally {
                    // 성공하든 실패하든 작업 완료 알림
                    doneLatch.countDown();
                }
            });
        }

        // 작업 등록이 끝난 뒤 실제 동시 실행 시간만 측정한다.
        long startTime = System.nanoTime();

        // 대기 중인 스레드들을 동시에 시작
        startLatch.countDown();

        // 모든 스레드의 작업이 끝날 때까지 대기
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);

        long endTime = System.nanoTime();
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // 테스트 종료 후 스레드 풀 종료
        executorService.shutdown();

        assertThat(completed).isTrue();

        // 테스트 종료 후 쿠폰 상태 재조회
        Coupon foundCoupon = couponRepository.findById(coupon.getId()).orElseThrow();

        // 실제 저장된 UserCoupon 개수 조회
        long issuedUserCouponCount = userCouponRepository.countByCouponId(coupon.getId());

        System.out.println("========== PESSIMISTIC LOCK 요청 수 증가 테스트 결과 ==========");
        System.out.println("총 요청 수 = " + requestCount);
        System.out.println("스레드 풀 크기 = " + threadPoolSize);
        System.out.println("성공 수 = " + successCount.get());
        System.out.println("실패 수 = " + failCount.get());
        System.out.println("쿠폰 소진 실패 수 = " + soldOutFailCount.get());
        System.out.println("기타 실패 수 = " + otherFailCount.get());
        System.out.println("쿠폰 총 수량 = " + foundCoupon.getTotalQuantity());
        System.out.println("쿠폰 잔여 수량 = " + foundCoupon.getRemainingQuantity());
        System.out.println("실제 UserCoupon 발급 개수 = " + issuedUserCouponCount);
        System.out.println("기대 잔여 수량 = " + (foundCoupon.getTotalQuantity() - issuedUserCouponCount));
        System.out.println("전체 처리 시간(ms) = " + elapsedMillis);
        System.out.println("==========================================================");

        // 실제 발급 개수는 쿠폰 총 수량과 일치
        assertThat(issuedUserCouponCount)
                .isEqualTo(foundCoupon.getTotalQuantity());

        // 성공 수는 쿠폰 총 수량과 일치
        assertThat(successCount.get())
                .isEqualTo(foundCoupon.getTotalQuantity());

        // 실패 수는 전체 요청 수에서 성공 수를 뺀 값과 일치
        assertThat(failCount.get())
                .isEqualTo(requestCount - successCount.get());

        // 실패한 요청은 전부 쿠폰 소진 실패여야 한다.
        assertThat(soldOutFailCount.get())
                .isEqualTo(failCount.get());

        // 쿠폰 소진 외 기타 실패는 없어야 한다.
        assertThat(otherFailCount.get())
                .isZero();

        // 쿠폰 잔여 수량은 0
        assertThat(foundCoupon.getRemainingQuantity())
                .isZero();

        // 실제 발급 개수와 쿠폰 잔여 수량 계산 결과 일치
        assertThat(foundCoupon.getRemainingQuantity())
                .isEqualTo(foundCoupon.getTotalQuantity() - issuedUserCouponCount);
    }

    // 쿠폰 소진 예외 발생 여부 확인
    private boolean isSoldOutException(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof CouponException couponException
                    && couponException.getErrorCode() == CouponErrorCode.COUPON_QUANTITY_EXHAUSTED) {
                return true;
            }

            throwable = throwable.getCause();
        }

        return false;
    }
}