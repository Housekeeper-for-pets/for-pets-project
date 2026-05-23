package com.forpets.domain.reservation.service;

import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

// CI 할 때 Redis 를 띄우는 게 따로 없어서
// Local desktop 에서 redis 를 docker 로 실행시킨 이후에 테스트를 진행해야합니다.
// CI 에 문제가 생기지 않도록 평소에는 Disabled 처리해둘게요!
@Disabled("결제 Lock Test: 락을 잡으려 할 때 1개만 성공하는지")
@SpringBootTest
@ActiveProfiles("test")
class ReservationLockIntegrationTest {

    @Autowired
    private ReservationLockService reservationLockService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final Long sitterProfileId = 100L;

    @AfterEach
    void cleanUp() {
        // 테스트 후 락 키 정리
        stringRedisTemplate.delete("lock:reservation:confirm:" + sitterProfileId);
    }

    @Test
    @DisplayName("[동시성] 5명이 동시에 같은 시터에 락 요청 — 1명만 성공")
    void concurrency_lock_test_01() throws InterruptedException {
        // given
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount); // 모든 스레드 준비 대기
        CountDownLatch startLatch = new CountDownLatch(1);           // 동시 출발 신호
        CountDownLatch doneLatch = new CountDownLatch(threadCount);  // 전체 완료 대기

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when — 5개 스레드가 동시에 락 획득 시도
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();  // 준비 완료 알림
                    startLatch.await();      // 출발 신호 대기

                    reservationLockService.executeWithSitterLock(
                            sitterProfileId,
                            () -> {
                                // 락 획득 성공 — 실제 confirm 로직 대신 sleep으로 점유
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                successCount.incrementAndGet();
                                return "confirmed";
                            });
                } catch (ReservationException e) {
                    if (e.getErrorCode() == ReservationErrorCode.RESERVATION_CONFIRM_LOCK_FAILED) {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 기타 예외 무시
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();   // 5개 스레드 모두 준비될 때까지 대기
        startLatch.countDown(); // 동시 출발!
        doneLatch.await();     // 전체 완료 대기

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(4);

        executor.shutdown();
    }

    @Test
    @DisplayName("[동시성] 락 해제 후 다음 요청 성공")
    void concurrency_lock_test_02() {
        // given — 첫 번째 요청 성공 후 락 해제
        String firstResult = reservationLockService.executeWithSitterLock(
                sitterProfileId, () -> "first");

        // when — 두 번째 요청
        String secondResult = reservationLockService.executeWithSitterLock(
                sitterProfileId, () -> "second");

        // then — 락 해제됐으므로 두 번째도 성공
        assertThat(firstResult).isEqualTo("first");
        assertThat(secondResult).isEqualTo("second");
    }

    @Test
    @DisplayName("[동시성] 다른 시터에 대한 락은 서로 독립적")
    void concurrency_lock_test_03() throws InterruptedException {
        // given
        Long sitterA = 100L;
        Long sitterB = 200L;
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);

        // when — 다른 시터에 동시에 락 요청
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                reservationLockService.executeWithSitterLock(sitterA, () -> {
                    successCount.incrementAndGet();
                    return "sitterA";
                });
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                reservationLockService.executeWithSitterLock(sitterB, () -> {
                    successCount.incrementAndGet();
                    return "sitterB";
                });
            } finally {
                doneLatch.countDown();
            }
        });

        doneLatch.await();

        // then — 둘 다 성공
        assertThat(successCount.get()).isEqualTo(2);

        // cleanup
        stringRedisTemplate.delete("lock:reservation:confirm:" + sitterB);
        executor.shutdown();
    }
}