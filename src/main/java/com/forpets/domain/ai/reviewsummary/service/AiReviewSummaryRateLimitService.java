package com.forpets.domain.ai.reviewsummary.service;

import com.forpets.domain.ai.reviewsummary.exception.AiReviewSummaryErrorCode;
import com.forpets.domain.ai.reviewsummary.exception.AiReviewSummaryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewSummaryRateLimitService {

    private static final String KEY_PREFIX = "rate-limit:ai-review-summary:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${forpets.ai.review-summary.rate-limit.max-requests:3}")
    private int maxRequests;

    @Value("${forpets.ai.review-summary.rate-limit.window-seconds:60}")
    private long windowSeconds;

    /**
     * 리뷰 요약 갱신 버튼 연타를 막기 위한 Redis 카운터입니다.
     * Redis 장애는 AI 보조 기능 전체 장애로 번지지 않도록 fail-open 처리합니다.
     */
    public void checkAllowed(Long memberId, Long sitterId) {
        String key = KEY_PREFIX + memberId + ":" + sitterId;

        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }

            if (count != null && count > maxRequests) {
                throw new AiReviewSummaryException(AiReviewSummaryErrorCode.AI_REVIEW_SUMMARY_RATE_LIMITED);
            }
        } catch (AiReviewSummaryException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("AI 리뷰 요약 rate limit 확인 실패. 요청은 허용합니다. memberId={}, sitterId={}",
                    memberId, sitterId, exception);
        }
    }
}
