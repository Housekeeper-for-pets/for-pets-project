package com.forpets.domain.ai.usage.service;

import com.forpets.domain.ai.usage.entity.*;
import com.forpets.domain.ai.usage.repository.AiUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiUsageLogService {

    private final AiUsageLogRepository aiUsageLogRepository;

    /**
     * AI 사용량 로그 저장 실패가 리뷰/추천 사용자 흐름을 막지 않도록 별도 트랜잭션에서 best-effort로 기록한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AiUsageRecord record) {
        try {
            aiUsageLogRepository.save(AiUsageLog.create(
                    record.feature(),
                    record.model(),
                    record.promptTokens(),
                    record.completionTokens(),
                    record.totalTokens(),
                    record.latencyMs(),
                    record.status(),
                    record.errorType(),
                    record.promptVersion(),
                    truncate(record.failureReason())
            ));
        } catch (Exception exception) {
            log.warn("AI 사용량 로그 저장 실패. feature={}, status={}, message={}",
                    record.feature(), record.status(), exception.getMessage());
        }
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 500) {
            return value;
        }
        return value.substring(0, 500);
    }
}
