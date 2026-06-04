package com.forpets.domain.ai.reviewsummary.service;

import com.forpets.domain.ai.reviewsummary.dto.ReviewSource;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.review.entity.Review;
import com.forpets.domain.review.repository.ReviewRepository;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ReviewRepositoryReviewSourceProviderTest {

    @InjectMocks
    private ReviewRepositoryReviewSourceProvider reviewSourceProvider;

    @Mock
    private SitterProfileRepository sitterProfileRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Test
    @DisplayName("[성공] 시터 프로필의 memberId로 최근 리뷰를 조회한다")
    void find_recent_reviews_by_sitter_id_success() {
        // given
        Long sitterId = 1L;
        Long sitterMemberId = 10L;
        SitterProfile sitterProfile = sitterProfile(sitterId, sitterMemberId);
        Review review = review(100L, sitterMemberId, 5, "정말 세심하게 돌봐주셔서 만족했습니다.");

        given(sitterProfileRepository.findById(sitterId)).willReturn(Optional.of(sitterProfile));
        given(reviewRepository.findAllActiveByRevieweeIdAndReservationStatus(
                org.mockito.ArgumentMatchers.eq(sitterMemberId),
                org.mockito.ArgumentMatchers.eq(ReservationStatus.COMPLETED),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(review)));

        // when
        List<ReviewSource> result = reviewSourceProvider.findRecentReviewsBySitterId(sitterId, 20);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).reviewId()).isEqualTo(100L);
        assertThat(result.get(0).rating()).isEqualTo(5);
        assertThat(result.get(0).content()).isEqualTo("정말 세심하게 돌봐주셔서 만족했습니다.");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(reviewRepository).should()
                .findAllActiveByRevieweeIdAndReservationStatus(
                        org.mockito.ArgumentMatchers.eq(sitterMemberId),
                        org.mockito.ArgumentMatchers.eq(ReservationStatus.COMPLETED),
                        pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
    }

    @Test
    @DisplayName("[실패] 시터 프로필이 없으면 SITTER_NOT_FOUND를 반환한다")
    void find_recent_reviews_by_sitter_id_sitter_not_found() {
        // given
        Long sitterId = 1L;
        given(sitterProfileRepository.findById(sitterId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewSourceProvider.findRecentReviewsBySitterId(sitterId, 20))
                .isInstanceOf(SitterException.class)
                .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                        .isEqualTo(SitterErrorCode.SITTER_NOT_FOUND));
    }

    private SitterProfile sitterProfile(Long sitterId, Long memberId) {
        SitterProfile sitterProfile = SitterProfile.builder()
                .memberId(memberId)
                .introduction("안녕하세요")
                .experienceYears(3)
                .possiblePetType(PossiblePetType.DOG)
                .possiblePetSize(PossiblePetSize.SMALL)
                .pricePerHour(15000)
                .build();
        ReflectionTestUtils.setField(sitterProfile, "id", sitterId);
        return sitterProfile;
    }

    private Review review(Long reviewId, Long revieweeId, Integer rating, String comment) {
        Review review = Review.builder()
                .reservationId(1L)
                .reviewerId(2L)
                .revieweeId(revieweeId)
                .reviewComment(comment)
                .rating(rating)
                .build();
        ReflectionTestUtils.setField(review, "id", reviewId);
        return review;
    }
}
