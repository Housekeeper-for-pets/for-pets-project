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
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 [트랜잭션 자원 낭비 검증]

 검증 대상 핵심 질문: @Transactional 진입 시점과 DB 커넥션 획득 시점이 동일한가?

 ※ 중요한 함정 (이 테스트의 @TestPropertySource 가 해결하는 것):
   Spring Boot 기본 설정에서 Hikari 는 autocommit=true 로 커넥션을 발급한다.
   Hibernate 는 JDBC 트랜잭션을 시작하려면 autocommit 을 false 로 토글해야 하고,
   토글하려면 커넥션을 잡아야 한다.
   → 결과적으로 hibernate.connection.handling_mode 가
      DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION (Spring Boot 기본) 이어도
      "@Transactional 진입 = autocommit 토글용 커넥션 획득" 이라 lazy 가 무력화됨.

   해결: 두 프로퍼티를 같이 켜야 진짜 lazy 가 된다.
     spring.datasource.hikari.auto-commit=false
     spring.jpa.properties.hibernate.connection.provider_disables_autocommit=true
   → 풀이 이미 autocommit=false 로 발급 → Hibernate 가 토글할 필요 없음
   → 첫 SQL 까지 커넥션 미점유

 그 위에서:

 케이스 A. @Transactional 진입 → (DB 접근 X) → tryLock 실패 → 즉시 rollback
   → 커넥션은 아예 안 잡혔으므로 낭비 0. 풀에 영향 없음.

 케이스 B. @Transactional 진입 → DB 조회 1회 → tryLock 실패 → rollback
   → 커넥션 점유한 채로 락 시도, 실패해도 rollback 까지 점유. 의미 없는 낭비 발생.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // lazy connection acquisition 을 진짜로 동작시키기 위한 필수 조합
        "spring.datasource.hikari.auto-commit=false",
        "spring.jpa.properties.hibernate.connection.provider_disables_autocommit=true"
})
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

        // when - DB 접근 없이 락 실패 시뮬레이트
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

        // when - findById 후 락 실패 시뮬레이트
        assertThatThrownBy(() -> probe.failLockAfterDbAccess(activeAtFailure, reservationId))
                .isInstanceOf(SimulatedLockFailure.class);

        // then
        // (1) 락 실패 시점에 커넥션이 이미 점유 중 -> rollback 비용까지 그대로 낭비됨
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
        private final ReservationRepository repository;
        private final HikariDataSource dataSource;

        ConnectionWasteProbeService(ReservationRepository repo, HikariDataSource ds) {
            this.repository = repo;
            this.dataSource = ds;
        }

        /** Case A: 트랜잭션 진입 -> DB 접근 X -> 락 실패 */
        @Transactional
        public void failLockWithoutDbAccess(AtomicInteger activeRecorder) {
            // 락 시도 직전 활성 커넥션 측정 — 이 시점까지 DB 문장이 0건이므로 미점유여야 함
            activeRecorder.set(dataSource.getHikariPoolMXBean().getActiveConnections());
            throw new SimulatedLockFailure();
        }

        /** Case B: 트랜잭션 진입 -> findById -> 락 실패 */
        @Transactional
        public void failLockAfterDbAccess(AtomicInteger activeRecorder, Long id) {
            repository.findById(id); // 첫 DB 문장 -> Hibernate 가 커넥션 획득
            activeRecorder.set(dataSource.getHikariPoolMXBean().getActiveConnections());
            throw new SimulatedLockFailure();
        }
    }
}
