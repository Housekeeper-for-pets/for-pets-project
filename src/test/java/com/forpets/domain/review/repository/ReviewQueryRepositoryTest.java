package com.forpets.domain.review.repository;

import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationSource;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.review.dto.SitterReviewStats;
import com.forpets.domain.review.entity.Review;
import com.forpets.global.common.CareType;
import com.forpets.global.config.QueryDslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, ReviewQueryRepository.class})
@DisplayName("시터 평점 집계 쿼리")
class ReviewQueryRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ReviewQueryRepository reviewQueryRepository;

    private static final Long SITTER_MEMBER_ID = 100L;

    @Test
    @DisplayName("[성공] deleted=false AND COMPLETED 리뷰만 평균/개수에 포함된다")
    void calculate_stats_includes_only_valid_reviews() {
        // given
        persistReview(persistReservation(ReservationStatus.COMPLETED), 4, false); // 포함
        persistReview(persistReservation(ReservationStatus.COMPLETED), 5, false); // 포함
        persistReview(persistReservation(ReservationStatus.COMPLETED), 1, true);  // 삭제 → 제외
        persistReview(persistReservation(ReservationStatus.PENDING), 2, false);   // 미완료 → 제외
        em.flush();
        em.clear();

        // when
        SitterReviewStats stats = reviewQueryRepository.calculateSitterReviewStats(SITTER_MEMBER_ID);

        // then
        assertThat(stats.averageRating()).isEqualByComparingTo("4.5");
        assertThat(stats.reviewCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("[성공] COMPLETED가 아닌 예약의 리뷰는 집계에서 제외된다")
    void calculate_stats_excludes_non_completed() {
        // given
        persistReview(persistReservation(ReservationStatus.COMPLETED), 4, false); // 포함
        persistReview(persistReservation(ReservationStatus.PENDING), 5, false);   // 미완료 → 제외
        em.flush();
        em.clear();

        // when
        SitterReviewStats stats = reviewQueryRepository.calculateSitterReviewStats(SITTER_MEMBER_ID);

        // then
        assertThat(stats.averageRating()).isEqualByComparingTo("4.0");
        assertThat(stats.reviewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("[성공] deleted=true 리뷰는 집계에서 제외된다")
    void calculate_stats_excludes_deleted() {
        // given
        persistReview(persistReservation(ReservationStatus.COMPLETED), 4, false); // 포함
        persistReview(persistReservation(ReservationStatus.COMPLETED), 5, true);  // 삭제 → 제외
        em.flush();
        em.clear();

        // when
        SitterReviewStats stats = reviewQueryRepository.calculateSitterReviewStats(SITTER_MEMBER_ID);

        // then
        assertThat(stats.averageRating()).isEqualByComparingTo("4.0");
        assertThat(stats.reviewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("[성공] 유효한 리뷰가 없으면 평균 0.0, 개수 0을 반환한다")
    void calculate_stats_zero_when_no_valid_reviews() {
        // given
        persistReview(persistReservation(ReservationStatus.COMPLETED), 5, true); // 삭제만 존재
        em.flush();
        em.clear();

        // when
        SitterReviewStats stats = reviewQueryRepository.calculateSitterReviewStats(SITTER_MEMBER_ID);

        // then
        assertThat(stats.averageRating()).isEqualByComparingTo("0.0");
        assertThat(stats.reviewCount()).isZero();
    }

    @Test
    @DisplayName("[성공] 소수점 평균은 첫째 자리에서 반올림된다")
    void calculate_stats_rounds_average() {
        // given (4, 4, 5 → 평균 4.333... → 4.3)
        persistReview(persistReservation(ReservationStatus.COMPLETED), 4, false);
        persistReview(persistReservation(ReservationStatus.COMPLETED), 4, false);
        persistReview(persistReservation(ReservationStatus.COMPLETED), 5, false);
        em.flush();
        em.clear();

        // when
        SitterReviewStats stats = reviewQueryRepository.calculateSitterReviewStats(SITTER_MEMBER_ID);

        // then
        assertThat(stats.averageRating()).isEqualByComparingTo("4.3");
        assertThat(stats.reviewCount()).isEqualTo(3);
    }

    private Reservation persistReservation(ReservationStatus status) {
        Reservation reservation = Reservation.builder()
                .guardianId(1L)
                .sitterMemberId(SITTER_MEMBER_ID)
                .sitterProfileId(10L)
                .careType(CareType.VISIT)
                .source(ReservationSource.CARE_REQUEST)
                .sourceId(1L)
                .build();
        if (status == ReservationStatus.COMPLETED) {
            reservation.confirm();
            reservation.complete();
        }
        em.persist(reservation);
        return reservation;
    }

    private void persistReview(Reservation reservation, int rating, boolean deleted) {
        Review review = Review.builder()
                .reservationId(reservation.getId())
                .reviewerId(reservation.getGuardianId())
                .revieweeId(reservation.getSitterMemberId())
                .reviewComment("정말 세심하게 돌봐주셔서 만족했습니다.")
                .rating(rating)
                .build();
        if (deleted) {
            review.delete();
        }
        em.persist(review);
    }
}
