package com.forpets.domain.carerequest.service;

import com.forpets.domain.carerequest.exception.CareRequestErrorCode;
import com.forpets.domain.carerequest.exception.CareRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/*
돌봄 요청에 대한 Redis 분산락
만료 스케줄러가 요청 상태를 변경하는 동안 동일 요청에 대한 다른 변경 작업이
끼어들지 못하도록 보호한다 (ReservationLockService 패턴 차용)
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class CareRequestLockService {
    private static final String CARE_REQUEST_LOCK_PREFIX = "lock:careRequest:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate stringRedisTemplate;

    public <T> T executeWithCareRequestLock(Long careRequestId, Supplier<T> task) {
        String lockKey = CARE_REQUEST_LOCK_PREFIX + careRequestId;
        String lockValue = UUID.randomUUID().toString();

        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TTL);

        if (!Boolean.TRUE.equals(acquired)) {
            throw new CareRequestException(CareRequestErrorCode.CARE_REQUEST_LOCK_FAILED);
        }

        try {
            return task.get();
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    private void releaseLock(String lockKey, String lockValue) {
        String savedValue = stringRedisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(savedValue)) {
            stringRedisTemplate.delete(lockKey);
        }
    }
}
