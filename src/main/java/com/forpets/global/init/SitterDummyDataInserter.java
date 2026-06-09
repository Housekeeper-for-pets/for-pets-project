package com.forpets.global.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * PET-214: 인덱스 효과 검증용 더미 데이터 삽입 — sitter_profile (건수: DummyDataConstants.TARGET_COUNT)
 *
 * 실행 조건: local 프로파일 + forpets.dummy-data.enabled=true (성능측정 시에만 활성화)
 *
 * [rewriteBatchedStatements 안내]
 * 배치 INSERT 성능을 최대화하려면 DataSource URL에 아래 파라미터를 추가하세요:
 *   ?...&rewriteBatchedStatements=true
 * application-local.yml 예시:
 *   url: jdbc:mysql://localhost:3307/for_pets?serverTimezone=Asia/Seoul
 *          &characterEncoding=UTF-8&useSSL=false
 *          &allowPublicKeyRetrieval=true
 *          &rewriteBatchedStatements=true
 *
 * [sitter_profile.member_id UNIQUE 제약 안내]
 * sitter_profile 테이블은 member_id에 UNIQUE 제약이 있어 member당 1개 프로필만 생성 가능합니다.
 * 따라서 sitter_profile 1만 건을 삽입하기 위해 더미 member 1만 건을 먼저 삽입합니다.
 *
 * [region 컬럼 안내]
 * PET-170 ERD에 sitter_profile.region 컬럼이 명시되어 있으나,
 * 현재 JPA 엔티티(SitterProfile)에는 해당 컬럼이 없습니다.
 * PET-170 마이그레이션으로 region 컬럼이 추가되면 insertSitterProfiles() SQL에 포함하세요.
 */
@Slf4j
@Component
@Profile("local")
@ConditionalOnProperty(name = "forpets.dummy-data.enabled", havingValue = "true")
@Order(2)
@RequiredArgsConstructor
public class SitterDummyDataInserter implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    private static final int TARGET_COUNT = DummyDataConstants.TARGET_COUNT;
    private static final int BATCH_SIZE = 1_000;
    private static final String DUMMY_EMAIL_PATTERN = "dummy-sitter-%d@dummy.local";
    private static final String DUMMY_INTRO = "더미 소개";

    private static final String[] MEMBER_REGIONS = {
            "GANGNAM", "SEOCHO", "SONGPA", "GANGDONG", "GANGBUK",
            "SEONGBUK", "DOBONG", "NOWON", "JONGNO", "JUNG",
            "YONGSAN", "EUNPYEONG", "SEODAEMUN", "MAPO", "GANGSEO",
            "YANGCHEON", "GURO", "GEUMCHEON", "YEONGDEUNGPO", "DONGJAK",
            "GWANAK", "DONGDAEMUN", "JUNGNANG", "SEONGDONG", "GWANGJIN"
    };
    private static final String[] GENDERS = {"MALE", "FEMALE", "UNKNOWN"};
    private static final String[] PET_TYPES = {"DOG", "CAT", "ETC", "ALL"};
    private static final String[] PET_SIZES = {"SMALL", "MEDIUM", "LARGE", "ALL"};

    @Override
    public void run(String... args) {
        Long existingDummySitters = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sitter_profile WHERE introduction = ?",
                Long.class, DUMMY_INTRO);
        if (existingDummySitters != null && existingDummySitters >= TARGET_COUNT) {
            log.info("[SitterDummyDataInserter] 더미 sitter_profile {}건 이미 존재. 스킵.", existingDummySitters);
            return;
        }

        // 1단계: 더미 member 삽입 (sitter_profile UNIQUE member_id 제약 대응)
        insertDummyMembers();

        // 2단계: 더미 member ID 목록 조회
        List<Long> dummyMemberIds = jdbcTemplate.queryForList(
                "SELECT id FROM member WHERE email LIKE 'dummy-sitter-%'", Long.class);
        log.info("[SitterDummyDataInserter] 더미 member {} 건 확인", dummyMemberIds.size());

        if (dummyMemberIds.isEmpty()) {
            log.warn("[SitterDummyDataInserter] 더미 member 없음. sitter_profile 삽입 불가.");
            return;
        }

        // 3단계: 이미 sitter_profile이 있는 member_id 제외
        List<Long> existingMemberIds = jdbcTemplate.queryForList(
                "SELECT member_id FROM sitter_profile", Long.class);
        Set<Long> existingSet = new HashSet<>(existingMemberIds);

        List<Long> available = new ArrayList<>();
        for (Long id : dummyMemberIds) {
            if (!existingSet.contains(id)) {
                available.add(id);
            }
        }

        int toInsert = Math.min(available.size(), TARGET_COUNT);
        if (toInsert == 0) {
            log.warn("[SitterDummyDataInserter] 삽입 가능한 member_id 없음. 스킵.");
            return;
        }

        // 4단계: sitter_profile 배치 삽입
        log.info("[SitterDummyDataInserter] sitter_profile {}건 배치 삽입 시작...", toInsert);
        insertSitterProfiles(available, toInsert);

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sitter_profile", Long.class);
        log.info("[SitterDummyDataInserter] 완료. sitter_profile COUNT(*) = {}", total);
    }

    private void insertDummyMembers() {
        Long existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member WHERE email LIKE 'dummy-sitter-%'", Long.class);
        long existing = existingCount == null ? 0 : existingCount;

        if (existing >= TARGET_COUNT) {
            log.info("[SitterDummyDataInserter] 더미 member 이미 {}건. member 삽입 스킵.", existing);
            return;
        }

        int remaining = (int) (TARGET_COUNT - existing);
        log.info("[SitterDummyDataInserter] 더미 member {}건 배치 삽입 시작...", remaining);

        Random random = new Random(42L);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneYearAgo = now.minusYears(1);

        // INSERT IGNORE: 재실행 시 email/nickname 중복으로 인한 오류 방지
        String sql = "INSERT IGNORE INTO member " +
                "(email, password, nickname, phone, region, gender, role, status, deleted, created_at, updated_at) " +
                "VALUES (?, 'DUMMY_NOT_LOGINABLE', ?, NULL, ?, ?, 'SITTER', 'ACTIVE', false, ?, ?)";

        for (int offset = 0; offset < remaining; offset += BATCH_SIZE) {
            int end = Math.min(offset + BATCH_SIZE, remaining);
            List<Object[]> batch = new ArrayList<>();
            for (int i = offset; i < end; i++) {
                long idx = existing + i + 1;
                LocalDateTime createdAt = randomDateTime(random, oneYearAgo, now);
                batch.add(new Object[]{
                        String.format(DUMMY_EMAIL_PATTERN, idx),
                        "더미시터" + idx,
                        MEMBER_REGIONS[(int) (idx % MEMBER_REGIONS.length)],
                        GENDERS[(int) (idx % GENDERS.length)],
                        createdAt,
                        randomDateTime(random, createdAt, now)
                });
            }
            jdbcTemplate.batchUpdate(sql, batch);
            log.info("[SitterDummyDataInserter] 더미 member {}/{} 삽입", (int) existing + end, (int) existing + remaining);
        }
    }

    private void insertSitterProfiles(List<Long> memberIds, int count) {
        Random random = new Random(7L);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneYearAgo = now.minusYears(1);

        // [region 컬럼 안내] sitter_profile에는 현재 region 컬럼이 없습니다.
        // PET-170 마이그레이션 후 "(region, ...)" 과 "?, ..." 를 SQL에 추가하세요.
        String sql = "INSERT INTO sitter_profile " +
                "(member_id, introduction, experience_years, possible_pet_type, possible_pet_size, " +
                "price_per_hour, average_rating, review_count, status, approval_status, " +
                "deleted, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, ?, ?)";

        for (int offset = 0; offset < count; offset += BATCH_SIZE) {
            int end = Math.min(offset + BATCH_SIZE, count);
            List<Object[]> batch = new ArrayList<>();
            for (int i = offset; i < end; i++) {
                Long memberId = memberIds.get(i);
                LocalDateTime createdAt = randomDateTime(random, oneYearAgo, now);
                // status: RESERVABLE 85% / NON_RESERVABLE 15%
                String status = random.nextDouble() < 0.85 ? "RESERVABLE" : "NON_RESERVABLE";
                // 실제값과 유사하게 변경 approval_status: APPROVED 75% / PENDING 15% / REJECTED 10%
                double r = random.nextDouble();//
                String approvalStatus = r < 0.75 ? "APPROVED"
                        : r < 0.90 ? "PENDING"
                        : "REJECTED";
                // average_rating: 0.0 ~ 5.0 (소수 첫째 자리)
                BigDecimal avgRating = BigDecimal.valueOf(Math.round(random.nextDouble() * 50) / 10.0);
                batch.add(new Object[]{
                        memberId,
                        DUMMY_INTRO,
                        random.nextInt(11),               // experience_years: 0~10
                        PET_TYPES[random.nextInt(4)],     // possible_pet_type
                        PET_SIZES[random.nextInt(4)],     // possible_pet_size
                        10000 + random.nextInt(40001),    // price_per_hour: 10000~50000
                        avgRating,                        // average_rating: 0.0~5.0
                        random.nextInt(301),              // review_count: 0~300
                        status,
                        approvalStatus,
                        createdAt,
                        randomDateTime(random, createdAt, now)
                });
            }
            jdbcTemplate.batchUpdate(sql, batch);
            log.info("[SitterDummyDataInserter] sitter_profile {}/{} 삽입", end, count);
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
