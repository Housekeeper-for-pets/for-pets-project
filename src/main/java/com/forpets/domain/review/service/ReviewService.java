package com.forpets.domain.review.service;

import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.domain.review.dto.CreateReviewRequest;
import com.forpets.domain.review.dto.ReviewResponse;
import com.forpets.domain.review.entity.Review;
import com.forpets.domain.review.exception.ReviewErrorCode;
import com.forpets.domain.review.exception.ReviewException;
import com.forpets.domain.review.repository.ReviewRepository;
import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MIN_COMMENT_LENGTH = 10;
    private static final int MAX_COMMENT_LENGTH = 500;

    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final BadWordFiltering badWordFiltering = new BadWordFiltering();

    @Transactional
    public ReviewResponse create(Long memberId, CreateReviewRequest request) {
        Reservation reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.RESERVATION_NOT_FOUND));

        validateCompleted(reservation);
        validateGuardian(memberId, reservation);
        validateNotReviewed(reservation.getId());
        validateRating(request.rating());
        String reviewComment = validateAndNormalizeComment(request.reviewComment());
        validateBadWord(reviewComment);

        Review review = reviewRepository.save(Review.builder()
                .reservationId(reservation.getId())
                .reviewerId(reservation.getGuardianId())
                .revieweeId(reservation.getSitterMemberId())
                .reviewComment(reviewComment)
                .rating(request.rating())
                .build());

        return ReviewResponse.from(review);
    }

    private void validateCompleted(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.COMPLETED) {
            throw new ReviewException(ReviewErrorCode.RESERVATION_NOT_COMPLETED);
        }
    }

    private void validateGuardian(Long memberId, Reservation reservation) {
        if (!reservation.isGuardian(memberId)) {
            throw new ReviewException(ReviewErrorCode.NOT_RESERVATION_GUARDIAN);
        }
    }

    private void validateNotReviewed(Long reservationId) {
        if (reviewRepository.existsByReservationId(reservationId)) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < MIN_RATING || rating > MAX_RATING) {
            throw new ReviewException(ReviewErrorCode.INVALID_RATING);
        }
    }

    private String validateAndNormalizeComment(String reviewComment) {
        if (reviewComment == null) {
            throw new ReviewException(ReviewErrorCode.INVALID_REVIEW_COMMENT);
        }

        String trimmedComment = reviewComment.trim();
        if (trimmedComment.isEmpty()
                || trimmedComment.length() < MIN_COMMENT_LENGTH
                || trimmedComment.length() > MAX_COMMENT_LENGTH) {
            throw new ReviewException(ReviewErrorCode.INVALID_REVIEW_COMMENT);
        }

        return trimmedComment;
    }

    private void validateBadWord(String reviewComment) {
        if (badWordFiltering.check(reviewComment)) {
            throw new ReviewException(ReviewErrorCode.CONTAIN_BAD_WORD);
        }
    }
}
