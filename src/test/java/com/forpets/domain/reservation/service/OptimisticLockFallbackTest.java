package com.forpets.domain.reservation.service;

import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationSource;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.global.common.CareType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/*
 [낙관락 마지막 방어선 검증]

 시나리오:
   분산 락이 어떤 이유로든 (TTL 만료, 락 키 누수, 신규 코드가 락 우회) 두 트랜잭션을 동시에 통과시켰다고 가정.
   (분산 락이 정상 동작하는 경로는 LockTransactionRaceConditionTest 의 [FIXED] 케이스)
   이 때 Reservation.@Version 이 한 쪽을 OptimisticLockException 으로 막아줄 수 있는지

 흐름:
   Thread A — tx 시작 → read (v=0) → [barrier] → confirm → commit (v=1)
   Thread B — tx 시작 → read (v=0) → [barrier] → [A commit 대기] → confirm → commit
              → DB 의 version 이 이미 1 이라 UPDATE ... WHERE version=0 가 0 rows
              → StaleObjectStateException → Spring 이 ObjectOptimisticLockingFailureException 으로 래핑
 */
@SpringBootTest
@ActiveProfiles("test")
class OptimisticLockFallbackTest {

    @Autowired private OptimisticLockProbe probe;
    @Autowired private ReservationRepository reservationRepository;

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
        reservationRepository.deleteAll();
    }

    @Test
    @DisplayName("[FALLBACK] 동시에 confirm 시도 — @Version 이 늦은 쪽을 OptimisticLock 으로 막는다")
    void optimistic_lock_blocks_second_writer() throws Exception {
        // given
        CyclicBarrier bothRead = new CyclicBarrier(2); // 두 스레드가 read 까지 완료한 시점에 동기화
        CountDownLatch aCommitted = new CountDownLatch(1); // A 의 commit 후 B 가 깨어남
        AtomicReference<Throwable> aError = new AtomicReference<>();
        AtomicReference<Throwable> bError = new AtomicReference<>();

        // when
        Thread tA = new Thread(() -> {
            try {
                probe.readBarrierConfirm(reservationId, bothRead);
            } catch (Throwable t) {
                aError.set(t);
            } finally {
                aCommitted.countDown(); // A 의 @Transactional 종료(commit/rollback) 이후에 시그널
            }
        });

        Thread tB = new Thread(() -> {
            try {
                probe.readBarrierWaitConfirm(reservationId, bothRead, aCommitted);
            } catch (Throwable t) {
                bError.set(t);
            }
        });

        tA.start();
        tB.start();
        tA.join();
        tB.join();

        // then
        // (1) A 는 성공
        assertThat(aError.get())
                .as("먼저 commit 한 A 는 정상 종료")
                .isNull();

        // (2) B 는 OptimisticLockException 으로 막힘
        assertThat(bError.get())
                .as("늦게 commit 시도한 B 는 version 불일치로 막힘")
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // (3) 최종 상태는 A 가 쓴 것 그대로, version 도 정확히 +1
        Reservation finalState = reservationRepository.findById(reservationId).orElseThrow();
        assertThat(finalState.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(finalState.getVersion()).isEqualTo(1L);
    }

    // ─────────────────────────────────────────────────────────────────
    // 테스트 전용 빈
    // ─────────────────────────────────────────────────────────────────

    @TestConfiguration
    static class Config {
        @Bean
        OptimisticLockProbe optimisticLockProbe(ReservationRepository repo) {
            return new OptimisticLockProbe(repo);
        }
    }

    /**
     * 분산 락이 없는 상황을 시뮬레이트하기 위해, 락 호출을 일부러 포함하지 않은 probe.
     * 두 스레드가 같은 reservation 을 동시에 confirm 한다.
     */
    static class OptimisticLockProbe {
        private final ReservationRepository repo;

        OptimisticLockProbe(ReservationRepository repo) {
            this.repo = repo;
        }

        /** A: read → 둘 다 read 한 시점 동기화 → confirm → (메서드 종료 시 commit) */
        @Transactional
        public void readBarrierConfirm(Long id, CyclicBarrier bothRead) throws Exception {
            Reservation r = repo.findById(id).orElseThrow();
            bothRead.await();
            r.confirm();
        }

        /** B: read → 동기화 → A 의 commit 이 끝날 때까지 대기 → confirm → (메서드 종료 시 commit 실패) */
        @Transactional
        public void readBarrierWaitConfirm(Long id, CyclicBarrier bothRead,
                                          CountDownLatch aCommitted) throws Exception {
            Reservation r = repo.findById(id).orElseThrow();
            bothRead.await();
            aCommitted.await();
            r.confirm();
            // 메서드 return 시점에 @Transactional advice 가 flush + commit 시도 ->
            // UPDATE ... WHERE version = 0 -> 0 rows -> StaleObjectStateException
            // -> Spring 이 ObjectOptimisticLockingFailureException 으로 래핑해서 던짐
        }
    }
}
