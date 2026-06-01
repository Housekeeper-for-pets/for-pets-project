package com.forpets.domain.ai.reviewsummary.service;

import com.forpets.domain.ai.reviewsummary.dto.ReviewSource;
import com.forpets.domain.review.entity.Review;
import com.forpets.domain.review.repository.ReviewRepository;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.context.annotation.Profile;
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
