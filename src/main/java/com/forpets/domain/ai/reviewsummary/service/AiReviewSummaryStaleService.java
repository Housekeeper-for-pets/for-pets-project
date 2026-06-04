package com.forpets.domain.ai.reviewsummary.service;

import com.forpets.domain.ai.reviewsummary.repository.SitterReviewSummaryRepository;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReviewSummaryStaleService {

    private final SitterProfileRepository sitterProfileRepository;
    private final SitterReviewSummaryRepository summaryRepository;

    @Transactional
    public void markStaleBySitterMemberId(Long sitterMemberId) {
        // Review는 시터 회원 ID를 들고 있고, AI 요약은 시터 프로필 ID를 기준으로 저장된다.
        sitterProfileRepository.findByMemberId(sitterMemberId)
                .flatMap(sitterProfile -> summaryRepository.findBySitterId(sitterProfile.getId()))
                .ifPresent(summary -> summary.markStale());
    }
}
