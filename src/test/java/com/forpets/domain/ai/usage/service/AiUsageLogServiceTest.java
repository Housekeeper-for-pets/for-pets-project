package com.forpets.domain.ai.usage.service;

import com.forpets.domain.ai.usage.entity.*;
import com.forpets.domain.ai.usage.repository.AiUsageLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AiUsageLogServiceTest {

    @InjectMocks
    private AiUsageLogService aiUsageLogService;

    @Mock
    private AiUsageLogRepository aiUsageLogRepository;

    @Test
    @DisplayName("[성공] AI 사용량 로그를 저장한다")
    void record_success() {
        // given
        AiUsageRecord record = AiUsageRecord.success(
                AiFeature.SITTER_REVIEW_SUMMARY,
                "gemini-test",
                100,
                50,
                150,
                1200L,
                "v1"
        );

        // when
        aiUsageLogService.record(record);

        // then
        then(aiUsageLogRepository).should().save(any(AiUsageLog.class));
    }

    @Test
    @DisplayName("[성공] AI 사용량 로그 저장 실패가 사용자 흐름으로 전파되지 않는다")
    void record_ignore_repository_failure() {
        // given
        AiUsageRecord record = AiUsageRecord.fallback(
                AiFeature.SITTER_RECOMMENDATION,
                "gemini-test",
                900L,
                AiErrorType.RATE_LIMIT,
                null,
                "429 Too Many Requests"
        );
        willThrow(new RuntimeException("db error")).given(aiUsageLogRepository).save(any(AiUsageLog.class));

        // when
        aiUsageLogService.record(record);

        // then
        then(aiUsageLogRepository).should().save(any(AiUsageLog.class));
    }
}
