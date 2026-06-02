package com.forpets.domain.review.repository;

import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.review.dto.MyReceivedReviewResponse;
import com.forpets.domain.review.dto.MyWrittenReviewResponse;
import com.forpets.domain.review.dto.SitterReviewStats;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static com.forpets.domain.member.entity.QMember.member;
import static com.forpets.domain.reservation.entity.QReservation.reservation;
import static com.forpets.domain.review.entity.QReview.review;
import static com.forpets.domain.sitter.entity.QSitterProfile.sitterProfile;

@Repository
@RequiredArgsConstructor
public class ReviewQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<MyWrittenReviewResponse> findMyWrittenReviews(Long reviewerId, Pageable pageable) {
        List<MyWrittenReviewResponse> content = queryFactory
                .select(Projections.constructor(
                        MyWrittenReviewResponse.class,
                        review.id,
                        review.reservationId,
                        review.revieweeId,
                        member.nickname,
                        sitterProfile.id,
                        review.rating,
                        review.reviewComment,
                        review.createdAt
                ))
                .from(review)
                .join(member).on(review.revieweeId.eq(member.id))
                .join(sitterProfile).on(member.id.eq(sitterProfile.memberId))
                .where(
                        review.reviewerId.eq(reviewerId),
                        review.deleted.isFalse()
                )
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = queryFactory
                .select(review.count())
                .from(review)
                .join(member).on(review.revieweeId.eq(member.id))
                .join(sitterProfile).on(member.id.eq(sitterProfile.memberId))
                .where(
                        review.reviewerId.eq(reviewerId),
                        review.deleted.isFalse()
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, count != null ? count : 0L);
    }

    public Page<MyReceivedReviewResponse> findMyReceivedReviews(Long revieweeId, Pageable pageable) {
        List<MyReceivedReviewResponse> content = queryFactory
                .select(Projections.constructor(
                        MyReceivedReviewResponse.class,
                        review.id,
                        review.reservationId,
                        review.reviewerId,
                        member.nickname,
                        review.rating,
                        review.reviewComment,
                        review.createdAt
                ))
                .from(review)
                .join(member).on(review.reviewerId.eq(member.id))
                .where(
                        review.revieweeId.eq(revieweeId),
                        review.deleted.isFalse()
                )
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = queryFactory
                .select(review.count())
                .from(review)
                .join(member).on(review.reviewerId.eq(member.id))
                .where(
                        review.revieweeId.eq(revieweeId),
                        review.deleted.isFalse()
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, count != null ? count : 0L);
    }

    /**
     * 시터 한 명의 평균 평점과 리뷰 수를 집계합니다.
     * 집계 대상: reviewee_id = sitterMemberId, deleted = false, reservation.status = COMPLETED
     * (review JOIN reservation 으로 COMPLETED 조건을 반드시 적용)
     * 리뷰가 0개이면 averageRating = 0.0, reviewCount = 0 으로 반환합니다.
     */
    public SitterReviewStats calculateSitterReviewStats(Long sitterMemberId) {
        Tuple result = queryFactory
                .select(review.rating.avg(), review.count())
                .from(review)
                .join(reservation).on(review.reservationId.eq(reservation.id))
                .where(
                        review.revieweeId.eq(sitterMemberId),
                        review.deleted.isFalse(),
                        reservation.status.eq(ReservationStatus.COMPLETED)
                )
                .fetchOne();

        Double avg = result != null ? result.get(review.rating.avg()) : null;
        Long count = result != null ? result.get(review.count()) : null;
        long reviewCount = count != null ? count : 0L;

        BigDecimal averageRating = (avg != null && reviewCount > 0)
                ? BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);

        return new SitterReviewStats(averageRating, reviewCount);
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (pageable.getSort().isUnsorted()) {
            return new OrderSpecifier[]{review.createdAt.desc()};
        }

        for (Sort.Order order : pageable.getSort()) {
            Order direction = order.getDirection().isAscending() ? Order.ASC : Order.DESC;
            switch (order.getProperty()) {
                case "createdAt" -> orderSpecifiers.add(new OrderSpecifier<>(direction, review.createdAt));
                case "rating" -> orderSpecifiers.add(new OrderSpecifier<>(direction, review.rating));
            }
        }
        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }
}
