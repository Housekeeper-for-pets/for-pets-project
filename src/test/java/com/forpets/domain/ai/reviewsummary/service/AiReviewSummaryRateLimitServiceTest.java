package com.forpets.domain.ai.reviewsummary.service;

import com.forpets.domain.ai.reviewsummary.exception.AiReviewSummaryErrorCode;
import com.forpets.domain.ai.reviewsummary.exception.AiReviewSummaryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AiReviewSummaryRateLimitServiceTest {

    @InjectMocks
    private AiReviewSummaryRateLimitService rateLimitService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("[성공] 허용 횟수 이내 요청은 통과한다")
    void check_allowed_within_limit() {
        // given
        setupLimit(3, 60);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("rate-limit:ai-review-summary:1:10")).willReturn(1L);

        // when & then
        assertThatCode(() -> rateLimitService.checkAllowed(1L, 10L))
                .doesNotThrowAnyException();
        then(stringRedisTemplate).should().expire("rate-limit:ai-review-summary:1:10", Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("[실패] 허용 횟수를 초과하면 429 예외를 던진다")
    void check_allowed_over_limit() {
        // given
        setupLimit(3, 60);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("rate-limit:ai-review-summary:1:10")).willReturn(4L);

        // when & then
        assertThatThrownBy(() -> rateLimitService.checkAllowed(1L, 10L))
                .isInstanceOf(AiReviewSummaryException.class)
                .satisfies(exception -> assertThat(((AiReviewSummaryException) exception).getErrorCode())
                        .isEqualTo(AiReviewSummaryErrorCode.AI_REVIEW_SUMMARY_RATE_LIMITED));
    }

    @Test
    @DisplayName("[성공] Redis 장애가 나도 AI 핵심 흐름을 막지 않는다")
    void check_allowed_fail_open_when_redis_failed() {
        // given
        setupLimit(3, 60);
        given(stringRedisTemplate.opsForValue()).willThrow(new IllegalStateException("redis down"));

        // when & then
        assertThatCode(() -> rateLimitService.checkAllowed(1L, 10L))
                .doesNotThrowAnyException();
    }

    private void setupLimit(int maxRequests, long windowSeconds) {
        ReflectionTestUtils.setField(rateLimitService, "maxRequests", maxRequests);
        ReflectionTestUtils.setField(rateLimitService, "windowSeconds", windowSeconds);
    }
}
