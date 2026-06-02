package com.forpets.domain.review.service;

import com.forpets.domain.ai.reviewsummary.service.AiReviewSummaryStaleService;
import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationSource;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.domain.review.dto.CreateReviewRequest;
import com.forpets.domain.review.dto.MyReceivedReviewPageResponse;
import com.forpets.domain.review.dto.MyReceivedReviewResponse;
import com.forpets.domain.review.dto.MyWrittenReviewPageResponse;
import com.forpets.domain.review.dto.MyWrittenReviewResponse;
import com.forpets.domain.review.dto.ReviewPageResponse;
import com.forpets.domain.review.dto.ReviewResponse;
import com.forpets.domain.review.dto.SitterReviewStats;
import com.forpets.domain.review.entity.Review;
import com.forpets.domain.review.exception.ReviewErrorCode;
import com.forpets.domain.review.exception.ReviewException;
import com.forpets.domain.review.repository.ReviewQueryRepository;
import com.forpets.domain.review.repository.ReviewRepository;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.domain.sitter.service.SitterCacheService;
import com.forpets.global.common.CareType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewQueryRepository reviewQueryRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SitterProfileRepository sitterProfileRepository;

    @Mock
    private SitterCacheService sitterCacheService;

    @Mock
    private AiReviewSummaryStaleService aiReviewSummaryStaleService;

    private final Long guardianId = 1L;
    private final Long sitterMemberId = 2L;
    private final Long otherMemberId = 3L;
    private final Long reservationId = 100L;
    private final Long sitterId = 50L;
    private Reservation completedReservation;
    private Reservation pendingReservation;

    @BeforeEach
    void setUp() {
        completedReservation = createReservation();
        completedReservation.confirm();
        completedReservation.complete();

        pendingReservation = createReservation();
    }

    @Nested
    @DisplayName("리뷰 작성")
    class CreateReviewTest {

        @Test
        @DisplayName("[성공] 보호자가 COMPLETED 예약에 리뷰를 작성한다")
        void create_review_success() {
            // given
            CreateReviewRequest request = new CreateReviewRequest(
                    reservationId,
                    "정말 세심하게 돌봐주셔서 만족했습니다.",
                    5
            );

            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(completedReservation));
            given(reviewRepository.existsByReservationId(reservationId)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> {
                Review review = invocation.getArgument(0);
                ReflectionTestUtils.setField(review, "id", 10L);
                return review;
            });

            // when
            ReviewResponse response = reviewService.create(guardianId, request);

            // then
            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.reservationId()).isEqualTo(reservationId);
            assertThat(response.reviewerId()).isEqualTo(guardianId);
            assertThat(response.revieweeId()).isEqualTo(sitterMemberId);
            assertThat(response.rating()).isEqualTo(5);
            then(aiReviewSummaryStaleService).should().markStaleBySitterMemberId(sitterMemberId);
        }

        @Test
        @DisplayName("[실패] 예약이 없으면 RESERVATION_NOT_FOUND를 반환한다")
        void create_review_reservation_not_found() {
            // given
            CreateReviewRequest request = validRequest();
            given(reservationRepository.findById(reservationId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.create(guardianId, request))
                    .isInstanceOf(ReviewException.class)
                    .satisfies(ex -> assertThat(((ReviewException) ex).getErrorCode())
                            .isEqualTo(ReviewErrorCode.RESERVATION_NOT_FOUND));
            then(reviewRepository).should(never()).existsByReservationId(any());
        }

        @Test
        @DisplayName("[실패] COMPLETED 상태가 아니면 RESERVATION_NOT_COMPLETED를 반환한다")
        void create_review_reservation_not_completed() {
            // given
            CreateReviewRequest request = validRequest();
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(pendingReservation));

            // when & then
            assertThatThrownBy(() -> reviewService.create(guardianId, request))
                    .isInstanceOf(ReviewException.class)
                    .satisfies(ex -> assertThat(((ReviewException) ex).getErrorCode())
                            .isEqualTo(ReviewErrorCode.RESERVATION_NOT_COMPLETED));
            then(reviewRepository).should(never()).existsByReservationId(any());
        }

        @Test
        @DisplayName("[실패] 보호자가 아니면 NOT_RESERVATION_GUARDIAN을 반환한다")
        void create_review_not_guardian() {
            // given
            CreateReviewRequest request = validRequest();
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(completedReservation));

            // when & then
            assertThatThrownBy(() -> reviewService.create(otherMemberId, request))
                    .isInstanceOf(ReviewException.class)
                    .satisfies(ex -> assertThat(((ReviewException) ex).getErrorCode())
                            .isEqualTo(ReviewErrorCode.NOT_RESERVATION_GUARDIAN));
            then(reviewRepository).should(never()).existsByReservationId(any());
        }

        @Test
        @DisplayName("[실패] 리뷰가 이미 있으면 REVIEW_ALREADY_EXISTS를 반환한다")
        void create_review_already_exists() {
            // given
            CreateReviewRequest request = validRequest();
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(completedReservation));
            given(reviewRepository.existsByReservationId(reservationId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> reviewService.create(guardianId, request))
                    .isInstanceOf(ReviewException.class)
                    .satisfies(ex -> assertThat(((ReviewException) ex).getErrorCode())
                            .isEqualTo(ReviewErrorCode.REVIEW_ALREADY_EXISTS));
            then(reviewRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("[실패] 평점이 범위를 벗어나면 INVALID_RATING을 반환한다")
        void create_review_invalid_rating() {
            // given
            CreateReviewRequest request = new CreateReviewRequest(
                    reservationId,
                    "정말 세심하게 돌봐주셔서 만족했습니다.",
                    6
            );
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(completedReservation));
            given(reviewRepository.existsByReservationId(reservationId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> reviewService.create(guardianId, request))
                    .isInstanceOf(ReviewException.class)
                    .satisfies(ex -> assertThat(((ReviewException) ex).getErrorCode())
                            .isEqualTo(ReviewErrorCode.INVALID_RATING));
            then(reviewRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("[실패] 리뷰 내용이 공백이면 INVALID_REVIEW_COMMENT를 반환한다")
        void create_review_invalid_comment() {
            // given
            CreateReviewRequest request = new CreateReviewRequest(reservationId, "          ", 5);
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(completedReservation));
            given(reviewRepository.existsByReservationId(reservationId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> reviewService.create(guardianId, request))
                    .isInstanceOf(ReviewException.class)
                    .satisfies(ex -> assertThat(((ReviewException) ex).getErrorCode())
                            .isEqualTo(ReviewErrorCode.INVALID_REVIEW_COMMENT));
            then(reviewRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("[실패] 금칙어가 포함되면 CONTAIN_BAD_WORD를 반환한다")
        void create_review_contain_bad_word() {
            // given
            CreateReviewRequest request = new CreateReviewRequest(
                    reservationId,
                    "진짜 씨발 별로였어요 다시는 안맡길거에요",
                    1
            );
            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(completedReservation));
            given(reviewRepository.existsByReservationId(reservationId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> reviewService.create(guardianId, request))
                    .isInstanceOf(ReviewException.class)
                    .satisfies(ex -> assertThat(((ReviewException) ex).getErrorCode())
                            .isEqualTo(ReviewErrorCode.CONTAIN_BAD_WORD));
            then(reviewRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("리뷰 삭제")
    class DeleteReviewTest {

        @Test
        @DisplayName("[성공] 작성자가 리뷰를 soft delete로 삭제한다")
        void delete_review_success() {
            // given
            Long reviewId = 10L;
            Review review = createReview(reviewId, guardianId);
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

            // when
            reviewService.delete(guardianId, reviewId);

            // then
            assertThat(review.isDeleted()).isTrue();
            then(reviewRepository).should(never()).save(any());
            then(aiReviewSummaryStaleService).should().markStaleBySitterMemberId(sitterMemberId);
        }

        @Test
        @DisplayName("[실패] 리뷰가 없으면 REVIEW_NOT_FOUND를 반환한다")
        void delete_review_not_found() {
            // given
            Long reviewId = 10L;
            given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.delete(guardianId, reviewId))
                    .isInstanceOf(ReviewException.class)
                    .satisfies(ex -> assertThat(((ReviewException) ex).getErrorCode())
                            .isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND));
        }

        @Test
        @DisplayName("[실패] 작성자가 아니면 NOT_REVIEW_AUTHOR를 반환한다")
        void delete_review_not_author() {
            // given
            Long reviewId = 10L;
            Review review = createReview(reviewId, guardianId);
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> reviewService.delete(otherMemberId, reviewId))
                    .isInstanceOf(ReviewException.class)
                    .satisfies(ex -> assertThat(((ReviewException) ex).getErrorCode())
                            .isEqualTo(ReviewErrorCode.NOT_REVIEW_AUTHOR));
            assertThat(review.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("[실패] 이미 삭제된 리뷰이면 REVIEW_ALREADY_DELETED를 반환한다")
        void delete_review_already_deleted() {
            // given
            Long reviewId = 10L;
            Review review = createReview(reviewId, guardianId);
            review.delete();
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> reviewService.delete(guardianId, reviewId))
                    .isInstanceOf(ReviewException.class)
                    .satisfies(ex -> assertThat(((ReviewException) ex).getErrorCode())
                            .isEqualTo(ReviewErrorCode.REVIEW_ALREADY_DELETED));
        }
    }

    @Nested
    @DisplayName("시터 리뷰 목록 조회")
    class GetSitterReviewsTest {

        @Test
        @DisplayName("[성공] 시터 프로필 ID로 삭제되지 않은 리뷰 목록을 조회한다")
        void get_sitter_reviews_success() {
            // given
            SitterProfile sitterProfile = createSitterProfile();
            Review review = createReview(10L, guardianId);

            given(sitterProfileRepository.findById(sitterId)).willReturn(Optional.of(sitterProfile));
            given(reviewRepository.findAllByRevieweeIdAndDeletedFalse(any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(review)));

            // when
            ReviewPageResponse response = reviewService.getSitterReviews(sitterId, 0, 10, "createdAt", "desc");

            // then
            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).revieweeId()).isEqualTo(sitterMemberId);
            assertThat(response.totalElements()).isEqualTo(1);
            assertThat(response.currentPage()).isEqualTo(0);
            assertThat(response.size()).isEqualTo(1);

            ArgumentCaptor<Long> revieweeIdCaptor = ArgumentCaptor.forClass(Long.class);
            then(reviewRepository).should()
                    .findAllByRevieweeIdAndDeletedFalse(revieweeIdCaptor.capture(), any(Pageable.class));
            assertThat(revieweeIdCaptor.getValue()).isEqualTo(sitterMemberId);
        }

        @Test
        @DisplayName("[성공] 리뷰가 없으면 빈 페이지를 반환한다")
        void get_sitter_reviews_empty() {
            // given
            SitterProfile sitterProfile = createSitterProfile();
            given(sitterProfileRepository.findById(sitterId)).willReturn(Optional.of(sitterProfile));
            given(reviewRepository.findAllByRevieweeIdAndDeletedFalse(any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of()));

            // when
            ReviewPageResponse response = reviewService.getSitterReviews(sitterId, 0, 10, "createdAt", "desc");

            // then
            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isZero();
        }

        @Test
        @DisplayName("[실패] 시터 프로필이 없으면 SITTER_NOT_FOUND를 반환한다")
        void get_sitter_reviews_sitter_not_found() {
            // given
            given(sitterProfileRepository.findById(sitterId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.getSitterReviews(sitterId, 0, 10, "createdAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.SITTER_NOT_FOUND));
        }

        @Test
        @DisplayName("[실패] page가 음수이면 INVALID_PAGE_REQUEST를 반환한다")
        void get_sitter_reviews_invalid_page() {
            assertThatThrownBy(() -> reviewService.getSitterReviews(sitterId, -1, 10, "createdAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_PAGE_REQUEST));
        }

        @Test
        @DisplayName("[실패] size가 허용 범위를 벗어나면 INVALID_PAGE_REQUEST를 반환한다")
        void get_sitter_reviews_invalid_size() {
            assertThatThrownBy(() -> reviewService.getSitterReviews(sitterId, 0, 51, "createdAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_PAGE_REQUEST));
        }

        @Test
        @DisplayName("[실패] sort가 허용되지 않은 필드이면 INVALID_SORT_FIELD를 반환한다")
        void get_sitter_reviews_invalid_sort() {
            assertThatThrownBy(() -> reviewService.getSitterReviews(sitterId, 0, 10, "updatedAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_SORT_FIELD));
        }

        @Test
        @DisplayName("[실패] direction이 asc/desc가 아니면 INVALID_SORT_FIELD를 반환한다")
        void get_sitter_reviews_invalid_direction() {
            assertThatThrownBy(() -> reviewService.getSitterReviews(sitterId, 0, 10, "rating", "random"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_SORT_FIELD));
        }
    }

    @Nested
    @DisplayName("내가 작성한 리뷰 목록 조회")
    class GetMyWrittenReviewsTest {

        @Test
        @DisplayName("[성공] MEMBER가 작성한 리뷰 목록을 조회한다")
        void get_my_written_reviews_member_success() {
            // given
            MyWrittenReviewResponse writtenReview = createMyWrittenReviewResponse(10L, sitterMemberId);
            given(reviewQueryRepository.findMyWrittenReviews(any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(writtenReview), PageRequest.of(0, 10), 1));

            // when
            MyWrittenReviewPageResponse response = reviewService.getMyWrittenReviews(
                    guardianId, 0, 10, "createdAt", "desc");

            // then
            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).revieweeNickname()).isEqualTo("시터닉네임");
            assertThat(response.content().get(0).sitterProfileId()).isEqualTo(sitterId);
            assertThat(response.totalElements()).isEqualTo(1);
            assertThat(response.currentPage()).isEqualTo(0);
            assertThat(response.size()).isEqualTo(10);

            ArgumentCaptor<Long> reviewerIdCaptor = ArgumentCaptor.forClass(Long.class);
            then(reviewQueryRepository).should()
                    .findMyWrittenReviews(reviewerIdCaptor.capture(), any(Pageable.class));
            assertThat(reviewerIdCaptor.getValue()).isEqualTo(guardianId);
        }

        @Test
        @DisplayName("[성공] SITTER도 보호자로서 작성한 리뷰 목록을 조회한다")
        void get_my_written_reviews_sitter_success() {
            // given
            MyWrittenReviewResponse writtenReview = createMyWrittenReviewResponse(11L, otherMemberId);
            given(reviewQueryRepository.findMyWrittenReviews(any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(writtenReview), PageRequest.of(0, 10), 1));

            // when
            MyWrittenReviewPageResponse response = reviewService.getMyWrittenReviews(
                    sitterMemberId, 0, 10, "rating", "asc");

            // then
            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).revieweeId()).isEqualTo(otherMemberId);

            ArgumentCaptor<Long> reviewerIdCaptor = ArgumentCaptor.forClass(Long.class);
            then(reviewQueryRepository).should()
                    .findMyWrittenReviews(reviewerIdCaptor.capture(), any(Pageable.class));
            assertThat(reviewerIdCaptor.getValue()).isEqualTo(sitterMemberId);
        }

        @Test
        @DisplayName("[성공] 내가 작성한 리뷰가 없으면 빈 페이지를 반환한다")
        void get_my_written_reviews_empty() {
            // given
            given(reviewQueryRepository.findMyWrittenReviews(any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            // when
            MyWrittenReviewPageResponse response = reviewService.getMyWrittenReviews(
                    guardianId, 0, 10, "createdAt", "desc");

            // then
            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isZero();
        }

        @Test
        @DisplayName("[실패] page가 음수이면 INVALID_PAGE_REQUEST를 반환한다")
        void get_my_written_reviews_invalid_page() {
            assertThatThrownBy(() -> reviewService.getMyWrittenReviews(
                    guardianId, -1, 10, "createdAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_PAGE_REQUEST));
        }

        @Test
        @DisplayName("[실패] size가 0이면 INVALID_PAGE_REQUEST를 반환한다")
        void get_my_written_reviews_zero_size() {
            assertThatThrownBy(() -> reviewService.getMyWrittenReviews(
                    guardianId, 0, 0, "createdAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_PAGE_REQUEST));
        }

        @Test
        @DisplayName("[실패] size가 51이면 INVALID_PAGE_REQUEST를 반환한다")
        void get_my_written_reviews_too_large_size() {
            assertThatThrownBy(() -> reviewService.getMyWrittenReviews(
                    guardianId, 0, 51, "createdAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_PAGE_REQUEST));
        }

        @Test
        @DisplayName("[실패] sort가 허용되지 않은 필드이면 INVALID_SORT_FIELD를 반환한다")
        void get_my_written_reviews_invalid_sort() {
            assertThatThrownBy(() -> reviewService.getMyWrittenReviews(
                    guardianId, 0, 10, "updatedAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_SORT_FIELD));
        }

        @Test
        @DisplayName("[실패] direction이 허용되지 않은 값이면 INVALID_SORT_FIELD를 반환한다")
        void get_my_written_reviews_invalid_direction() {
            assertThatThrownBy(() -> reviewService.getMyWrittenReviews(
                    guardianId, 0, 10, "createdAt", "random"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_SORT_FIELD));
        }
    }

    @Nested
    @DisplayName("내가 받은 리뷰 목록 조회")
    class GetMyReceivedReviewsTest {

        @Test
        @DisplayName("[성공] SITTER가 받은 리뷰 목록을 조회한다")
        void get_my_received_reviews_sitter_success() {
            // given
            MyReceivedReviewResponse receivedReview = createMyReceivedReviewResponse(10L, guardianId);
            given(reviewQueryRepository.findMyReceivedReviews(any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(receivedReview), PageRequest.of(0, 10), 1));

            // when
            MyReceivedReviewPageResponse response = reviewService.getMyReceivedReviews(
                    sitterMemberId, 0, 10, "createdAt", "DESC");

            // then
            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).reviewerId()).isEqualTo(guardianId);
            assertThat(response.content().get(0).reviewerNickname()).isEqualTo("보호자닉네임");
            assertThat(response.totalElements()).isEqualTo(1);
            assertThat(response.currentPage()).isEqualTo(0);
            assertThat(response.size()).isEqualTo(10);

            ArgumentCaptor<Long> revieweeIdCaptor = ArgumentCaptor.forClass(Long.class);
            then(reviewQueryRepository).should()
                    .findMyReceivedReviews(revieweeIdCaptor.capture(), any(Pageable.class));
            assertThat(revieweeIdCaptor.getValue()).isEqualTo(sitterMemberId);
        }

        @Test
        @DisplayName("[성공] MEMBER가 호출하면 빈 페이지를 반환한다")
        void get_my_received_reviews_member_empty() {
            // given
            given(reviewQueryRepository.findMyReceivedReviews(any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            // when
            MyReceivedReviewPageResponse response = reviewService.getMyReceivedReviews(
                    guardianId, 0, 10, "createdAt", "desc");

            // then
            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isZero();

            ArgumentCaptor<Long> revieweeIdCaptor = ArgumentCaptor.forClass(Long.class);
            then(reviewQueryRepository).should()
                    .findMyReceivedReviews(revieweeIdCaptor.capture(), any(Pageable.class));
            assertThat(revieweeIdCaptor.getValue()).isEqualTo(guardianId);
        }

        @Test
        @DisplayName("[성공] 받은 리뷰가 없으면 빈 페이지를 반환한다")
        void get_my_received_reviews_empty() {
            // given
            given(reviewQueryRepository.findMyReceivedReviews(any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            // when
            MyReceivedReviewPageResponse response = reviewService.getMyReceivedReviews(
                    sitterMemberId, 0, 10, "createdAt", "desc");

            // then
            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isZero();
        }

        @Test
        @DisplayName("[실패] page가 음수이면 INVALID_PAGE_REQUEST를 반환한다")
        void get_my_received_reviews_invalid_page() {
            assertThatThrownBy(() -> reviewService.getMyReceivedReviews(
                    sitterMemberId, -1, 10, "createdAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_PAGE_REQUEST));
        }

        @Test
        @DisplayName("[실패] size가 0이면 INVALID_PAGE_REQUEST를 반환한다")
        void get_my_received_reviews_zero_size() {
            assertThatThrownBy(() -> reviewService.getMyReceivedReviews(
                    sitterMemberId, 0, 0, "createdAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_PAGE_REQUEST));
        }

        @Test
        @DisplayName("[실패] size가 51이면 INVALID_PAGE_REQUEST를 반환한다")
        void get_my_received_reviews_too_large_size() {
            assertThatThrownBy(() -> reviewService.getMyReceivedReviews(
                    sitterMemberId, 0, 51, "createdAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_PAGE_REQUEST));
        }

        @Test
        @DisplayName("[실패] sort가 허용되지 않은 필드이면 INVALID_SORT_FIELD를 반환한다")
        void get_my_received_reviews_invalid_sort() {
            assertThatThrownBy(() -> reviewService.getMyReceivedReviews(
                    sitterMemberId, 0, 10, "updatedAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_SORT_FIELD));
        }

        @Test
        @DisplayName("[실패] direction이 허용되지 않은 값이면 INVALID_SORT_FIELD를 반환한다")
        void get_my_received_reviews_invalid_direction() {
            assertThatThrownBy(() -> reviewService.getMyReceivedReviews(
                    sitterMemberId, 0, 10, "createdAt", "ascending"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_SORT_FIELD));
        }

        @Test
        @DisplayName("[실패] direction이 한글 값이면 INVALID_SORT_FIELD를 반환한다")
        void get_my_received_reviews_korean_invalid_direction() {
            assertThatThrownBy(() -> reviewService.getMyReceivedReviews(
                    sitterMemberId, 0, 10, "createdAt", "내림차순"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_SORT_FIELD));
        }
    }

    @Nested
    @DisplayName("리뷰 작성/삭제 시 시터 평점 갱신")
    class UpdateSitterReviewStatsTest {

        @Test
        @DisplayName("[성공] 리뷰 작성 시 SitterProfile 평점/리뷰 수가 갱신되고 상세 캐시가 무효화된다")
        void update_stats_on_create() {
            // given
            CreateReviewRequest request = validRequest();
            SitterProfile sitterProfile = createSitterProfile();

            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(completedReservation));
            given(reviewRepository.existsByReservationId(reservationId)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(sitterProfileRepository.findByMemberId(sitterMemberId)).willReturn(Optional.of(sitterProfile));
            given(reviewQueryRepository.calculateSitterReviewStats(sitterMemberId))
                    .willReturn(new SitterReviewStats(new BigDecimal("4.5"), 2));

            // when
            reviewService.create(guardianId, request);

            // then
            assertThat(sitterProfile.getAverageRating()).isEqualByComparingTo("4.5");
            assertThat(sitterProfile.getReviewCount()).isEqualTo(2);
            then(sitterCacheService).should().evictSitterDetail(sitterId);
        }

        @Test
        @DisplayName("[성공] 리뷰 삭제 시 평점/리뷰 수가 재계산되어 갱신된다")
        void update_stats_on_delete() {
            // given
            Long reviewId = 10L;
            Review review = createReview(reviewId, guardianId);
            SitterProfile sitterProfile = createSitterProfile();

            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
            given(sitterProfileRepository.findByMemberId(sitterMemberId)).willReturn(Optional.of(sitterProfile));
            given(reviewQueryRepository.calculateSitterReviewStats(sitterMemberId))
                    .willReturn(new SitterReviewStats(new BigDecimal("3.0"), 1));

            // when
            reviewService.delete(guardianId, reviewId);

            // then
            assertThat(review.isDeleted()).isTrue();
            assertThat(sitterProfile.getAverageRating()).isEqualByComparingTo("3.0");
            assertThat(sitterProfile.getReviewCount()).isEqualTo(1);
            then(sitterCacheService).should().evictSitterDetail(sitterId);
        }

        @Test
        @DisplayName("[성공] 마지막 리뷰 삭제로 리뷰가 0개가 되면 평점 0.0, 리뷰 수 0으로 갱신된다")
        void update_stats_to_zero_when_no_reviews() {
            // given
            Long reviewId = 10L;
            Review review = createReview(reviewId, guardianId);
            SitterProfile sitterProfile = createSitterProfile();

            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
            given(sitterProfileRepository.findByMemberId(sitterMemberId)).willReturn(Optional.of(sitterProfile));
            given(reviewQueryRepository.calculateSitterReviewStats(sitterMemberId))
                    .willReturn(new SitterReviewStats(new BigDecimal("0.0"), 0));

            // when
            reviewService.delete(guardianId, reviewId);

            // then
            assertThat(sitterProfile.getAverageRating()).isEqualByComparingTo("0.0");
            assertThat(sitterProfile.getReviewCount()).isZero();
            then(sitterCacheService).should().evictSitterDetail(sitterId);
        }

        @Test
        @DisplayName("[성공] 시터 프로필이 없으면(소프트 삭제) 평점 갱신을 건너뛴다")
        void skip_stats_when_profile_absent() {
            // given
            Long reviewId = 10L;
            Review review = createReview(reviewId, guardianId);

            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
            given(sitterProfileRepository.findByMemberId(sitterMemberId)).willReturn(Optional.empty());

            // when
            reviewService.delete(guardianId, reviewId);

            // then
            assertThat(review.isDeleted()).isTrue();
            then(reviewQueryRepository).should(never()).calculateSitterReviewStats(any());
            then(sitterCacheService).should(never()).evictSitterDetail(any());
        }
    }

    private Reservation createReservation() {
        Reservation reservation = Reservation.builder()
                .guardianId(guardianId)
                .sitterMemberId(sitterMemberId)
                .sitterProfileId(50L)
                .careType(CareType.VISIT)
                .source(ReservationSource.CARE_REQUEST)
                .sourceId(70L)
                .build();
        ReflectionTestUtils.setField(reservation, "id", reservationId);
        return reservation;
    }

    private CreateReviewRequest validRequest() {
        return new CreateReviewRequest(
                reservationId,
                "정말 세심하게 돌봐주셔서 만족했습니다.",
                5
        );
    }

    private Review createReview(Long reviewId, Long reviewerId) {
        Review review = Review.builder()
                .reservationId(reservationId)
                .reviewerId(reviewerId)
                .revieweeId(sitterMemberId)
                .reviewComment("정말 세심하게 돌봐주셔서 만족했습니다.")
                .rating(5)
                .build();
        ReflectionTestUtils.setField(review, "id", reviewId);
        return review;
    }

    private SitterProfile createSitterProfile() {
        SitterProfile sitterProfile = SitterProfile.builder()
                .memberId(sitterMemberId)
                .introduction("반려동물을 세심하게 돌보는 시터입니다.")
                .experienceYears(3)
                .possiblePetType(PossiblePetType.ALL)
                .possiblePetSize(PossiblePetSize.ALL)
                .pricePerHour(15000)
                .build();
        ReflectionTestUtils.setField(sitterProfile, "id", sitterId);
        return sitterProfile;
    }

    private MyWrittenReviewResponse createMyWrittenReviewResponse(Long reviewId, Long revieweeId) {
        return new MyWrittenReviewResponse(
                reviewId,
                reservationId,
                revieweeId,
                "시터닉네임",
                sitterId,
                5,
                "정말 세심하게 돌봐주셔서 만족했습니다.",
                null
        );
    }

    private MyReceivedReviewResponse createMyReceivedReviewResponse(Long reviewId, Long reviewerId) {
        return new MyReceivedReviewResponse(
                reviewId,
                reservationId,
                reviewerId,
                "보호자닉네임",
                5,
                "정말 세심하게 돌봐주셔서 만족했습니다.",
                null
        );
    }
}
