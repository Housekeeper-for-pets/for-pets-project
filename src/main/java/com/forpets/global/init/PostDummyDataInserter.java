package com.forpets.global.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * PET-214: 인덱스 효과 검증용 더미 데이터 삽입 — post (건수: DummyDataConstants.TARGET_COUNT)
 *
 * 실행 조건: local 프로파일 + forpets.dummy-data.enabled=true (성능측정 시에만 활성화)
 * SitterDummyDataInserter(@Order(2)) 이후에 실행됩니다.
 *
 * [rewriteBatchedStatements 안내]
 * 배치 INSERT 성능을 최대화하려면 DataSource URL에 아래 파라미터를 추가하세요:
 *   ?...&rewriteBatchedStatements=true
 *
 * [region / view_count 컬럼 안내]
 * PET-170/PET-171 ERD에 post.region, post.view_count 컬럼이 명시되어 있으나,
 * 현재 JPA 엔티티(Post)에는 해당 컬럼이 없습니다.
 * 마이그레이션 후 insertPosts() SQL에 해당 컬럼을 추가하세요.
 *
 * [author_id 안내]
 * 실제 테이블의 작성자 컬럼명은 member_id (Post 엔티티 기준)입니다.
 */
@Slf4j
@Component
@Profile("local")
@ConditionalOnProperty(name = "forpets.dummy-data.enabled", havingValue = "true")
@Order(3)
@RequiredArgsConstructor
public class PostDummyDataInserter implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    private static final int TARGET_COUNT = DummyDataConstants.TARGET_COUNT;
    private static final int BATCH_SIZE = 1_000;
    private static final String[] CARE_TYPES = {"VISIT", "BOARDING"};

    @Override
    public void run(String... args) {
        Long existingDummy = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM post WHERE title LIKE '더미 공고 %'", Long.class);
        long existing = existingDummy == null ? 0 : existingDummy;

        if (existing >= TARGET_COUNT) {
            log.info("[PostDummyDataInserter] 더미 post {}건 이미 존재. 스킵.", existing);
            return;
        }

        // author_id(=member_id)는 DB에서 기존 member id 목록을 SELECT해서 랜덤 매핑
        List<Long> memberIds = jdbcTemplate.queryForList("SELECT id FROM member", Long.class);
        if (memberIds.isEmpty()) {
            log.warn("[PostDummyDataInserter] member 테이블이 비어 있어 post 삽입 불가. 스킵.");
            return;
        }

        log.info("[PostDummyDataInserter] member {} 건 기준으로 post {}건 배치 삽입 시작...",
                memberIds.size(), TARGET_COUNT - existing);
        insertPosts(memberIds, existing);

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post", Long.class);
        log.info("[PostDummyDataInserter] 완료. post COUNT(*) = {}", total);
    }

    private void insertPosts(List<Long> memberIds, long alreadyInserted) {
        Random random = new Random(13L);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneYearAgo = now.minusYears(1);
        int memberCount = memberIds.size();

        // [region / view_count 컬럼 안내] 현재 post 엔티티에 해당 컬럼 없음.
        // PET-170/PET-171 마이그레이션 후 SQL 및 VALUES에 추가하세요:
        //   INSERT INTO post (member_id, title, content, care_type, budget_amount, status,
        //                     region, view_count, deleted, created_at, updated_at)
        //   VALUES (?, ?, '더미 내용', ?, ?, ?, ?, ?, false, ?, ?)
        String sql = "INSERT INTO post " +
                "(member_id, title, content, care_type, budget_amount, status, deleted, created_at, updated_at) " +
                "VALUES (?, ?, '더미 내용', ?, ?, ?, false, ?, ?)";

        int remaining = (int) (TARGET_COUNT - alreadyInserted);

        for (int offset = 0; offset < remaining; offset += BATCH_SIZE) {
            int end = Math.min(offset + BATCH_SIZE, remaining);
            List<Object[]> batch = new ArrayList<>();
            for (int i = offset; i < end; i++) {
                long n = alreadyInserted + i + 1;
                Long memberId = memberIds.get(random.nextInt(memberCount));
                LocalDateTime createdAt = randomDateTime(random, oneYearAgo, now);
                // 변경 후 (OPEN 10% / CLOSED 90% — 실서비스 현실 분포)
                String status = random.nextDouble() < 0.10 ? "OPEN" : "CLOSED";
                // care_type: VISIT / BOARDING 랜덤
                String careType = CARE_TYPES[random.nextInt(2)];
                batch.add(new Object[]{
                        memberId,
                        "더미 공고 " + n,                    // title: "더미 공고 {n}" 형태
                        careType,
                        10000 + random.nextInt(90001),     // budget_amount: 10000~100000
                        status,
                        createdAt,
                        randomDateTime(random, createdAt, now)
                });
            }
            jdbcTemplate.batchUpdate(sql, batch);
            log.info("[PostDummyDataInserter] post {}/{} 삽입",
                    (int) alreadyInserted + end, TARGET_COUNT);
        }
    }

    private LocalDateTime randomDateTime(Random random, LocalDateTime from, LocalDateTime to) {
        long fromEpoch = from.toEpochSecond(ZoneOffset.UTC);
        long toEpoch = to.toEpochSecond(ZoneOffset.UTC);
        if (toEpoch <= fromEpoch) return from;
        long epoch = fromEpoch + (long) (random.nextDouble() * (toEpoch - fromEpoch));
        return LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC);
    }
}
