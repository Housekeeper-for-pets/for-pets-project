package com.forpets.domain.review.service;

import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.domain.review.dto.CreateReviewRequest;
import com.forpets.domain.review.dto.MyReceivedReviewPageResponse;
import com.forpets.domain.review.dto.MyReceivedReviewResponse;
import com.forpets.domain.review.dto.MyWrittenReviewPageResponse;
import com.forpets.domain.review.dto.MyWrittenReviewResponse;
import com.forpets.domain.review.dto.ReviewPageResponse;
import com.forpets.domain.review.dto.ReviewResponse;
import com.forpets.domain.review.entity.Review;
import com.forpets.domain.review.exception.ReviewErrorCode;
import com.forpets.domain.review.exception.ReviewException;
import com.forpets.domain.review.repository.ReviewQueryRepository;
import com.forpets.domain.review.repository.ReviewRepository;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MIN_COMMENT_LENGTH = 10;
    private static final int MAX_COMMENT_LENGTH = 500;

    private final ReviewRepository reviewRepository;
    private final ReviewQueryRepository reviewQueryRepository;
    private final ReservationRepository reservationRepository;
    private final SitterProfileRepository sitterProfileRepository;
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

    @Transactional
    public void delete(Long memberId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.REVIEW_NOT_FOUND));

        validateAuthor(memberId, review);
        validateNotDeleted(review);

        review.delete();
    }

    public ReviewPageResponse getSitterReviews(Long sitterId, int page, int size, String sort, String direction) {
        validatePageRequest(page, size);
        validateSortField(sort);
        validateSortDirection(direction);

        SitterProfile sitterProfile = sitterProfileRepository.findById(sitterId)
                .orElseThrow(() -> new SitterException(SitterErrorCode.SITTER_NOT_FOUND));

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<Review> reviewPage = reviewRepository.findAllByRevieweeIdAndDeletedFalse(
                sitterProfile.getMemberId(), pageable);

        return ReviewPageResponse.of(
                reviewPage.getContent().stream()
                        .map(ReviewResponse::from)
                        .toList(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages(),
                reviewPage.getNumber(),
                reviewPage.getSize()
        );
    }

    public MyWrittenReviewPageResponse getMyWrittenReviews(Long memberId, int page, int size,
                                                           String sort, String direction) {
        validatePageRequest(page, size);
        validateSortField(sort);
        validateSortDirection(direction);

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<MyWrittenReviewResponse> reviewPage = reviewQueryRepository.findMyWrittenReviews(memberId, pageable);

        return MyWrittenReviewPageResponse.of(
                reviewPage.getContent(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages(),
                reviewPage.getNumber(),
                reviewPage.getSize()
        );
    }

    public MyReceivedReviewPageResponse getMyReceivedReviews(Long memberId, int page, int size,
                                                             String sort, String direction) {
        validatePageRequest(page, size);
        validateSortField(sort);
        validateSortDirection(direction);

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<MyReceivedReviewResponse> reviewPage = reviewQueryRepository.findMyReceivedReviews(memberId, pageable);

        return MyReceivedReviewPageResponse.of(
                reviewPage.getContent(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages(),
                reviewPage.getNumber(),
                reviewPage.getSize()
        );
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

    private void validateAuthor(Long memberId, Review review) {
        if (!review.isAuthor(memberId)) {
            throw new ReviewException(ReviewErrorCode.NOT_REVIEW_AUTHOR);
        }
    }

    private void validateNotDeleted(Review review) {
        if (review.isDeleted()) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_DELETED);
        }
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "rating"
    );

    private static final Set<String> ALLOWED_SORT_DIRECTIONS = Set.of(
            "asc", "desc"
    );

    private void validateSortField(String sort) {
        if (!ALLOWED_SORT_FIELDS.contains(sort)) {
            throw new SitterException(SitterErrorCode.INVALID_SORT_FIELD);
        }
    }

    private void validateSortDirection(String direction) {
        if (!ALLOWED_SORT_DIRECTIONS.contains(direction.toLowerCase())) {
            throw new SitterException(SitterErrorCode.INVALID_SORT_FIELD);
        }
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new SitterException(SitterErrorCode.INVALID_PAGE_REQUEST);
        }
        if (size < 1 || size > 50) {
            throw new SitterException(SitterErrorCode.INVALID_PAGE_REQUEST);
        }
    }
}
