package com.forpets.domain.review.dto;

import java.math.BigDecimal;

/**
 * 시터 한 명의 평점 집계 결과입니다.
 * deleted = false 이고 reservation.status = COMPLETED 인 리뷰만 대상으로 계산됩니다.
 * 리뷰가 0개이면 averageRating = 0.0, reviewCount = 0 으로 반환됩니다.
 */
public record SitterReviewStats(
        BigDecimal averageRating,
        long reviewCount
) {
}
