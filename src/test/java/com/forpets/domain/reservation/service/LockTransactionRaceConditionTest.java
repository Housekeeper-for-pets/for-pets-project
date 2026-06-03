package com.forpets.domain.reservation.service;

import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationSource;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.global.common.CareType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/*
 Lock - Transaction race condition 증명용 통합 테스트.

 시나리오 (Thread A 가 expire, Thread B 가 같은 reservation 을 조회):

  [ANTI-PATTERN]  락이 @Transactional 안쪽
    A: tx 시작 → 락 획득 → expire() → 락 해제 → (commit 전 비즈니스 로직 ≒ sleep) → commit
    B: A 가 락 해제하자마자 락 획득 → 조회
       → B 는 A 의 commit 이전 시점이므로 PENDING (stale) 을 읽음. 정합성 깨짐.

  [FIXED]  락이 @Transactional 바깥 (현재 운영 코드)
    A: 락 획득 → (tx 시작 → expire() → commit) → 락 해제
    B: A 가 락 해제하자마자 락 획득 → 조회
       → B 시점엔 이미 commit 완료. EXPIRED 를 읽음.

 운영 코드에서 락-tx 경계를 뒤집는 두 가지 방법은 동등함:
   (a) Service 분리: ReservationLockService.executeWithReservationLock(...)
                    → ReservationExpireService.expireOne(...)  [@Transactional]
   (b) AOP: @DistributedLock + @Order(HIGHEST_PRECEDENCE)
            → @Transactional advice 보다 바깥에서 락 advice 가 적용됨.

 Redis 필요. CI 에서는 @Disabled.
 */
@Disabled("로컬에서 Redis 띄운 후 실행 — Lock/Transaction race condition 증명")
@SpringBootTest
@ActiveProfiles("test")
class LockTransactionRaceConditionTest {

    private static final String LOCK_INSIDE_TX_KEY_PREFIX = "lock:inside-tx:";
    private static final String CORRECT_LOCK_KEY_PREFIX = "lock:reservation:";

    @Autowired private LockInsideTxExpireService lockInsideTxExpireService;
    @Autowired private LockOutsideReader readerService;
    @Autowired private ReservationLockService reservationLockService;
    @Autowired private ReservationExpireService reservationExpireService;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private StringRedisTemplate redis;

    private Long reservationId;

    @BeforeEach
    void setup() {
        Reservation r = Reservation.builder()
                .guardianId(1L)
                .sitterMemberId(2L)
                .sitterProfileId(100L)
                .careType(CareType.VISIT)
                .source(ReservationSource.CARE_REQUEST)
                .sourceId(200L)
                .build();
        reservationId = reservationRepository.save(r).getId();
    }

    @AfterEach
    void cleanup() {
        redis.delete(LOCK_INSIDE_TX_KEY_PREFIX + reservationId);
        redis.delete(CORRECT_LOCK_KEY_PREFIX + reservationId);
        reservationRepository.deleteAll();
    }

    @Test
    @DisplayName("[ANTI-PATTERN] 락이 트랜잭션 안쪽 — B 가 commit 전 PENDING 을 읽는다")
    void race_condition_when_lock_inside_transaction() throws Exception {
        // given
        CountDownLatch aReleasedLock = new CountDownLatch(1);
        AtomicReference<ReservationStatus> bSaw = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        // when
        Thread tA = new Thread(() -> {
            try {
                lockInsideTxExpireService.expireWithLockInside(reservationId, aReleasedLock);
            } catch (Throwable t) {
                error.set(t);
            }
        });

        Thread tB = new Thread(() -> {
            try {
                aReleasedLock.await(); // A 가 락 해제할 때까지 대기 (이 시점 A 는 아직 commit 전)
                bSaw.set(lockInsideTxExpireService.readUnderLock(reservationId));
            } catch (Throwable t) {
                error.set(t);
            }
        });

        tA.start();
        tB.start();
        tA.join();
        tB.join();

        // then
        assertThat(error.get()).isNull();
        // 핵심: A 가 expire() 를 호출했음에도 commit 전이라 B 는 PENDING 을 본다.
        assertThat(bSaw.get())
                .as("락이 트랜잭션 안쪽이면 B 는 A 의 commit 이전 상태(PENDING)를 읽는다")
                .isEqualTo(ReservationStatus.PENDING);

        // 끝나고 나서 보면 A 의 commit 은 결국 일어남 → DB 는 EXPIRED.
        // 즉, "B 가 본 것" 과 "최종 DB 상태" 가 어긋났던 순간이 존재했다는 증거.
        ReservationStatus finalDbStatus = readerService.read(reservationId);
        assertThat(finalDbStatus).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    @DisplayName("[FIXED] 락이 트랜잭션 바깥 — B 는 commit 된 EXPIRED 를 읽는다")
    void no_race_when_lock_outside_transaction() throws Exception {
        // given
        CountDownLatch aAcquiredLock = new CountDownLatch(1);
        AtomicReference<ReservationStatus> bSaw = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        // when
        Thread tA = new Thread(() -> {
            try {
                reservationLockService.executeWithReservationLock(reservationId, () -> {
                    aAcquiredLock.countDown(); // 락 잡았다고 B 한테 알림
                    reservationExpireService.expireOne(reservationId); // @Transactional - 락 안쪽에서 시작/commit
                    return null;
                });
            } catch (Throwable t) {
                error.set(t);
            }
        });

        Thread tB = new Thread(() -> {
            try {
                aAcquiredLock.await();
                // A 가 락 풀 때까지 폴링. 락이 풀린 시점 = A 의 commit 이 끝난 시점이므로
                // B 가 락을 잡는 순간 DB 는 이미 EXPIRED.
                ReservationStatus seen = acquireLockWithRetry(reservationId);
                bSaw.set(seen);
            } catch (Throwable t) {
                error.set(t);
            }
        });

        tA.start();
        tB.start();
        tA.join();
        tB.join();

        // then
        assertThat(error.get()).isNull();
        assertThat(bSaw.get())
                .as("락이 트랜잭션 바깥이면 B 는 항상 commit 된 상태(EXPIRED)를 본다")
                .isEqualTo(ReservationStatus.EXPIRED);
    }

    private ReservationStatus acquireLockWithRetry(Long id) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                return reservationLockService.executeWithReservationLock(id,
                        () -> readerService.read(id));
            } catch (Exception ignored) {
                Thread.sleep(10);
            }
        }
        throw new IllegalStateException("락 획득 실패 (timeout)");
    }

    // ─────────────────────────────────────────────────────────────────
    // 테스트 전용 빈
    // ─────────────────────────────────────────────────────────────────

    @TestConfiguration
    static class Config {
        @Bean
        LockInsideTxExpireService lockInsideTxExpireService(ReservationRepository repo, StringRedisTemplate redis) {
            return new LockInsideTxExpireService(repo, redis);
        }

        @Bean
        LockOutsideReader lockOutsideReader(ReservationRepository repo) {
            return new LockOutsideReader(repo);
        }
    }

    /**
     * 안티패턴: @Transactional 메서드 내부에서 락을 잡고/해제한다.
     * 락 해제는 메서드 return 직전 finally 에서 일어나지만, commit 은 메서드 return 이후에 일어난다.
     * 그래서 "락 해제 ~ commit" 사이 윈도우가 생기고, 그 윈도우에 다른 스레드가 끼어들 수 있음.
     */
    static class LockInsideTxExpireService {
        private final ReservationRepository repository;
        private final StringRedisTemplate redisTemplate;

        LockInsideTxExpireService(ReservationRepository repository, StringRedisTemplate redisTemplate) {
            this.repository = repository;
            this.redisTemplate = redisTemplate;
        }

        @Transactional
        public void expireWithLockInside(Long id, CountDownLatch lockReleasedSignal) {
            String key = LOCK_INSIDE_TX_KEY_PREFIX + id;
            String value = UUID.randomUUID().toString();
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(10));
            if (!Boolean.TRUE.equals(acquired)) {
                throw new IllegalStateException("A: 락 획득 실패");
            }

            try {
                Reservation r = repository.findById(id).orElseThrow();
                r.expire(); // PENDING → EXPIRED (영속성 컨텍스트에만 반영, 아직 commit 아님)
            } finally {
                // 안티패턴 포인트: 락이 commit 이전에 풀린다.
                if (value.equals(redisTemplate.opsForValue().get(key))) {
                    redisTemplate.delete(key);
                }
                lockReleasedSignal.countDown(); // B 를 깨움

                // "락 해제 ~ commit" 사이에 실제로 일어날 수 있는 비즈니스 로직 (네트워크 IO, GC pause, 등)
                // 을 시뮬레이트. 짧아도 충분.
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // 메서드 종료 → @Transactional advice 가 commit
        }

        @Transactional(readOnly = true)
        public ReservationStatus readUnderLock(Long id) {
            String key = LOCK_INSIDE_TX_KEY_PREFIX + id;
            String value = UUID.randomUUID().toString();
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(10));
            if (!Boolean.TRUE.equals(acquired)) {
                throw new IllegalStateException("B: 락 획득 실패");
            }
            try {
                return repository.findById(id).orElseThrow().getStatus();
            } finally {
                if (value.equals(redisTemplate.opsForValue().get(key))) {
                    redisTemplate.delete(key);
                }
            }
        }
    }

    /** 락 바깥에서 호출되는 단순 reader. 자체 read-only 트랜잭션을 시작해서 commit 된 값만 본다. */
    static class LockOutsideReader {
        private final ReservationRepository repo;

        LockOutsideReader(ReservationRepository repo) {
            this.repo = repo;
        }

        @Transactional(readOnly = true)
        public ReservationStatus read(Long id) {
            return repo.findById(id).orElseThrow().getStatus();
        }
    }
}
