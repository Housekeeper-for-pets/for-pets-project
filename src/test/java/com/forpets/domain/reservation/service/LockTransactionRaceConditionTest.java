package com.forpets.domain.reservation.service;

import com.forpets.domain.reservation.entity.*;
import com.forpets.domain.reservation.exception.ReservationException;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/*
 Lock - Transaction 사이 race condition 증명 테스트

 시나리오 : Thread A 가 expire, Thread B 가 동일 Reservation 을 cancel 하려고 함

  [ANTI-PATTERN]  락이 @Transactional 안쪽
    A: tx 시작 -> (Lock 획득 -> findById(v=0) -> expire() -> Lock 해제) -> sleep(300) -> commit 시도
    B: A 가 락 해제 직후 -> tx 시작 -> findById(v=0, A 가 아직 commit 안 함) -> cancel() -> commit (v=1)
    A: sleep 끝 -> commit 시도 -> UPDATE ... WHERE version=0 이 0 rows -> StaleObjectStateException
       -> Spring 이 ObjectOptimisticLockingFailureException 으로 래핑

    결과:
      - B 는 stale PENDING 을 읽고 cancel 까지 commit 했다 (직접적인 race 증거)
      - A 의 expire 는 @Version 에 의해 손실됨 -> 환불/proposal 복구가 누락될 수 있음
      - 즉 락 직렬화 실패가 결국 @Version 까지 닿아서 운영 사고로 이어진다

  [FIXED]  락이 @Transactional 바깥 (현재 운영 코드)
    A: 락 획득 -> (tx 시작 -> expire() -> commit) -> 락 해제
    B: A 의 락 해제 후에야 락 획득 가능 -> 그 시점엔 이미 EXPIRED commit 됨 -> cancel 거부
 */

// redis 가 필요하므로 CI 에서는 안 돌게 disabled 처리
//@Disabled("로컬에서 Redis 띄운 후 실행 — Lock/Transaction race condition 증명")
@SpringBootTest
@ActiveProfiles("test")
class LockTransactionRaceConditionTest {

    // anti pattern 에서 사용하는 Lock key prefix
    private static final String LOCK_INSIDE_TX_KEY_PREFIX = "lock:inside-tx:";

    // 현재 운영 코드에서 사용하는 Lock key prefix
    private static final String CORRECT_LOCK_KEY_PREFIX = "lock:reservation:";

    @Autowired private LockInsideTxExpireService lockInsideTxExpireService;
    @Autowired private LockOutsideReader readerService;
    @Autowired private CancelService cancelService;
    @Autowired private ReservationLockService reservationLockService;
    @Autowired private ReservationExpireService reservationExpireService;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private StringRedisTemplate redis;

    private Long reservationId;

    // reservation 생성, reservation Id 가져옴
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

    // lock key 만들어진거 delete, reservationRepository 도 비워줌
    @AfterEach
    void cleanup() {
        redis.delete(LOCK_INSIDE_TX_KEY_PREFIX + reservationId);
        redis.delete(CORRECT_LOCK_KEY_PREFIX + reservationId);
        reservationRepository.deleteAll();
    }

    @Test
    @DisplayName("[ANTI-PATTERN] 락이 트랜잭션 안쪽 — B는 stale PENDING으로 cancel commit, A의 expire는 @Version에 막혀 손실됨")
    void race_condition_when_lock_inside_transaction() throws Exception {
        CountDownLatch aReleasedLock = new CountDownLatch(1);
        AtomicReference<Throwable> aError = new AtomicReference<>();
        AtomicReference<Throwable> bError = new AtomicReference<>();
        AtomicBoolean bCommitted = new AtomicBoolean(false);

        // A: expire (락 해제 후 sleep -> 나중에 commit 시도)
        Thread tA = new Thread(() -> {
            try {
                lockInsideTxExpireService.expireWithLockInside(reservationId, aReleasedLock);
            } catch (Throwable t) {
                aError.set(t);
            }
        });

        // B: A 가 락 해제하자마자 -> stale PENDING 읽고 cancel commit
        Thread tB = new Thread(() -> {
            try {
                aReleasedLock.await();
                cancelService.cancel(reservationId);
                bCommitted.set(true); // B 입장에선 정상 종료
            } catch (Throwable t) {
                bError.set(t);
            }
        });

        tA.start();
        tB.start();
        tA.join();
        tB.join();

        // (1) B 는 PENDING 을 읽고 cancel 까지 commit 했다 — 락 직렬화 실패의 직접 증거
        assertThat(bError.get()).isNull();
        assertThat(bCommitted.get())
                .as("B 가 A 의 commit 이전 상태(PENDING)를 보고 cancel 을 commit 했다")
                .isTrue();

        // (2) A 의 commit 은 @Version 이 막아서 OptimisticLockException
        //     == race 가 @Version 까지 도달했다 == 분산 락만으론 못 막은 케이스
        assertThat(aError.get())
                .as("A 의 expire 가 @Version 에 의해 막힘 → 환불/proposal 복구가 누락될 수 있음")
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // (3) 최종 DB 는 B 의 cancel 이 살아남고, A 의 expire 는 흔적 없음
        Reservation saved = reservationRepository.findById(reservationId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(saved.getCanceledBy()).isNotNull();
        assertThat(saved.getCanceledAt()).isNotNull();
        assertThat(saved.getCancelReason()).isNotNull();
        // expire 의 흔적 (expiredAt) 은 남지 않음 -> 만료 처리 자체가 사라짐
        assertThat(saved.getExpiredAt()).isNull();
    }

    @Test
    @DisplayName("[FIXED] 락이 트랜잭션 바깥 — B는 EXPIRED를 읽고 취소 자체가 거부된다")
    void no_race_when_lock_outside_transaction() throws Exception {
        CountDownLatch aAcquiredLock = new CountDownLatch(1);
        AtomicReference<Throwable> bError = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        // A: 락 획득 → expireOne (commit) → 락 해제
        Thread tA = new Thread(() -> {
            try {
                reservationLockService.executeWithReservationLock(reservationId, () -> {
                    aAcquiredLock.countDown();
                    reservationExpireService.expireOne(reservationId);
                    return null;
                });
            } catch (Throwable t) {
                error.set(t);
            }
        });

        // B: A 락 해제 후 락 획득 -> 이미 EXPIRED -> cancel 거부
        Thread tB = new Thread(() -> {
            try {
                aAcquiredLock.await();
                acquireLockAndCancel(reservationId);
            } catch (Throwable t) {
                bError.set(t); // 이건 기대하는 예외
            }
        });

        tA.start();
        tB.start();
        tA.join();
        tB.join();

        assertThat(error.get()).isNull(); // A는 정상
        assertThat(bError.get())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("락 획득 타임아웃");

        Reservation saved = reservationRepository.findById(reservationId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.EXPIRED);

        // 데이터가 오염되지 않았다
        assertThat(saved.getCanceledBy()).isNull();
        assertThat(saved.getCanceledAt()).isNull();
        assertThat(saved.getCancelReason()).isNull();
    }

    private void acquireLockAndCancel(Long id) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                reservationLockService.executeWithReservationLock(id,
                        () -> cancelService.cancel(id));
                return;
            } catch (ReservationException e) {
                // 락 획득 실패 (A가 아직 안 풀었음) → 재시도
                Thread.sleep(10);
            }
            // ReservationException 이 아닌 예외 (INVALID_STATUS 등) 는 위로 전파
        }
        throw new IllegalStateException("락 획득 타임아웃");
    }

    // ─────────────────────────────────────────────────────────────────
    // 안티패턴 테스트 전용 빈
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

        @Bean
        CancelService cancelService(ReservationRepository repo) {
            return new CancelService(repo);
        }
    }

    /**
     * 안티패턴: @Transactional 메서드 내부에서 락을 잡고/해제
     * 락 해제는 메서드 return 직전 finally 에서 일어나지만, commit 은 메서드 return 이후에 일어난다.
     * 그래서 락 해제 ~ commit 사이 시간 차이가 생김 == 다른 스레드가 끼어들 수 있다
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
                r.expire(); // PENDING -> EXPIRED (영속성 컨텍스트에만 반영, 아직 commit 아님)
            } finally {
                // 안티패턴 포인트: 락이 commit 이전에 풀린다.
                if (value.equals(redisTemplate.opsForValue().get(key))) {
                    redisTemplate.delete(key);
                }
                lockReleasedSignal.countDown(); // B 를 깨움

                // 락 해제 ~ commit 사이에 실제로 일어날 수 있는 비즈니스 로직 대신 sleep 쓰기
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // 메서드 종료 → @Transactional advice 가 commit
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

    static class CancelService {
        private final ReservationRepository repository;

        CancelService(ReservationRepository repository) {
            this.repository = repository;
        }

        // 예약 취소 로직
        @Transactional
        public Void cancel(Long id) {
            Reservation r = repository.findById(id).orElseThrow();
            r.cancel("개인 사정으로 취소합니다", CancelCategory.PERSONAL, CanceledBy.GUARDIAN);
            return null;
        }
    }
}
