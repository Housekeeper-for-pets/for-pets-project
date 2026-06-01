package com.forpets.domain.ai.reviewsummary.service;

import com.forpets.domain.ai.reviewsummary.dto.ReviewSource;
import com.forpets.domain.review.entity.Review;
import com.forpets.domain.review.repository.ReviewRepository;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!stub-review")
@RequiredArgsConstructor
public class ReviewRepositoryReviewSourceProvider implements ReviewSourceProvider {

    private final SitterProfileRepository sitterProfileRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public List<ReviewSource> findRecentReviewsBySitterId(Long sitterId, int limit) {
        // Review는 시터 프로필 ID가 아니라 시터 회원 ID(revieweeId)를 기준으로 저장된다.
        SitterProfile sitterProfile = sitterProfileRepository.findById(sitterId)
                .orElseThrow(() -> new SitterException(SitterErrorCode.SITTER_NOT_FOUND));

        PageRequest pageRequest = PageRequest.of(
                0,
                limit,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return reviewRepository.findAllByRevieweeIdAndDeletedFalse(sitterProfile.getMemberId(), pageRequest)
                .getContent()
                .stream()
                .map(this::toSource)
                .toList();
    }

    private ReviewSource toSource(Review review) {
        return new ReviewSource(
                review.getId(),
                review.getRating(),
                review.getReviewComment()
        );
    }
}
