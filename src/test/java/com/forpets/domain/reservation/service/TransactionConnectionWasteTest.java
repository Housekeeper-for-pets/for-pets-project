package com.forpets.domain.reservation.service;

import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationSource;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.global.common.CareType;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 [트랜잭션 자원 낭비 검증]

 핵심 질문:
   "@Transactional 진입 시점" 과 "DB 커넥션 획득 시점" 이 같은가?

 Spring + Hibernate 기본 설정 (hibernate.connection.handling_mode =
 DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION) 에서는
   - 트랜잭션 진입 시점엔 커넥션을 안 잡고
   - 첫 DB 문장이 나갈 때 커넥션을 점유하고
   - commit / rollback 시점에 반환한다.
 즉 lazy connection acquisition.

 그래서 락이 트랜잭션 안쪽에 있을 때 커넥션 낭비 정도는 "락 시도 시점에 DB 접근을 했느냐" 에 달려있음.

 케이스 A. @Transactional 진입 → (DB 접근 X) → tryLock 실패 → 즉시 rollback
   → 커넥션은 아예 안 잡혔으므로 낭비 0. 풀에 영향 없음.

 케이스 B. @Transactional 진입 → DB 조회 1회 → tryLock 실패 → rollback
   → 커넥션 점유한 채로 락 시도, 실패해도 rollback 까지 점유. 의미 없는 낭비 발생.

 결론:
   - 락 실패 가능성이 있는 흐름이라면 트랜잭션 진입 전에 락을 잡아야 한다.
     (ReservationLockService 로 분리 / @DistributedLock + @Order(HIGHEST_PRECEDENCE))
   - 분리 전엔 케이스 B 패턴이 발생할 수 있었고, 분리 후엔 락 실패 시 아예 @Transactional
     안으로 들어가지 않으므로 케이스 A 보다도 안전 (트랜잭션 동기화 셋업조차 안 일어남).

 본 테스트는 락 자체는 시뮬레이트 (예외 throw) 하므로 Redis 가 필요하지 않다.
 */
@SpringBootTest
@ActiveProfiles("test")
class TransactionConnectionWasteTest {

    @Autowired private ConnectionWasteProbeService probe;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private HikariDataSource hikariDataSource;

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
    @DisplayName("[A] @Transactional 진입만으론 커넥션 미획득 — DB 접근 전 락 실패 = 낭비 0")
    void connection_not_acquired_when_lock_fails_before_db_access() {
        // given
        HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
        int baseline = pool.getActiveConnections();
        AtomicInteger activeAtFailure = new AtomicInteger(-1);

        // when — DB 접근 없이 락 실패 시뮬레이트
        assertThatThrownBy(() -> probe.failLockWithoutDbAccess(activeAtFailure))
                .isInstanceOf(SimulatedLockFailure.class);

        // then
        // (1) 락 실패 직전, 트랜잭션은 진입했지만 커넥션은 아직 안 잡혀 있었음
        assertThat(activeAtFailure.get())
                .as("DB 접근 전이면 @Transactional 진입만으론 커넥션 획득 X")
                .isEqualTo(baseline);
        // (2) 사후에도 baseline 유지
        assertThat(pool.getActiveConnections()).isEqualTo(baseline);
    }

    @Test
    @DisplayName("[B] DB 조회 후 락 실패 — 커넥션 점유 + 무의미한 rollback (낭비 발생)")
    void connection_acquired_and_wasted_when_lock_fails_after_db_access() {
        // given
        HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
        int baseline = pool.getActiveConnections();
        AtomicInteger activeAtFailure = new AtomicInteger(-1);

        // when — findById 후 락 실패 시뮬레이트
        assertThatThrownBy(() -> probe.failLockAfterDbAccess(activeAtFailure, reservationId))
                .isInstanceOf(SimulatedLockFailure.class);

        // then
        // (1) 락 실패 시점에 커넥션이 이미 점유 중 → rollback 비용까지 그대로 낭비됨
        assertThat(activeAtFailure.get())
                .as("DB 접근 후엔 커넥션 점유. 락 실패해도 rollback 까지 못 풀어줌")
                .isEqualTo(baseline + 1);
        // (2) rollback 종료 시점엔 풀로 반환됨
        assertThat(pool.getActiveConnections()).isEqualTo(baseline);
    }

    // ────────────────────────────────────────────────────────────
    // 테스트 전용 빈 + 시뮬레이션 예외
    // ────────────────────────────────────────────────────────────

    /** 실제 Redis 락 실패 대신 던지는 예외. 락 시도 시점을 결정적으로 재현하기 위함. */
    static class SimulatedLockFailure extends RuntimeException {
        SimulatedLockFailure() { super("simulated tryLock failure"); }
    }

    @TestConfiguration
    static class Config {
        @Bean
        ConnectionWasteProbeService connectionWasteProbeService(
                ReservationRepository repo, HikariDataSource ds) {
            return new ConnectionWasteProbeService(repo, ds);
        }
    }

    /**
     * 트랜잭션 안쪽에서 락을 잡는 패턴을 단순화한 probe.
     * 실제 락 API 를 호출하는 대신 SimulatedLockFailure 를 throw 하여 "락 획득 실패" 를 흉내낸다.
     * 시점 측정 (activeRecorder) 은 락 시도 직전, 즉 throw 직전에 한다.
     */
    static class ConnectionWasteProbeService {
        private final ReservationRepository repo;
        private final HikariDataSource ds;

        ConnectionWasteProbeService(ReservationRepository repo, HikariDataSource ds) {
            this.repo = repo;
            this.ds = ds;
        }

        /** Case A: 트랜잭션 진입 → DB 접근 X → 락 실패 */
        @Transactional
        public void failLockWithoutDbAccess(AtomicInteger activeRecorder) {
            // 락 시도 직전 활성 커넥션 측정 — 이 시점까지 DB 문장이 0건이므로 미점유여야 함
            activeRecorder.set(ds.getHikariPoolMXBean().getActiveConnections());
            throw new SimulatedLockFailure();
        }

        /** Case B: 트랜잭션 진입 → findById → 락 실패 */
        @Transactional
        public void failLockAfterDbAccess(AtomicInteger activeRecorder, Long id) {
            repo.findById(id); // 첫 DB 문장 → Hibernate 가 커넥션 획득
            activeRecorder.set(ds.getHikariPoolMXBean().getActiveConnections());
            throw new SimulatedLockFailure();
        }
    }
}
