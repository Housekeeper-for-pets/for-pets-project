package com.forpets.domain.payment.service;

import com.forpets.domain.payment.exception.PaymentErrorCode;
import com.forpets.domain.payment.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class PaymentLockService {
    private static final String PAYMENT_LOCK_PREFIX = "lock:payment:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate stringRedisTemplate;

    public <T> T executeWithReservationLock(Long reservationId, Supplier<T> task) {
        String lockKey = PAYMENT_LOCK_PREFIX + reservationId;
        String lockValue = UUID.randomUUID().toString();

        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TTL);

        if (!Boolean.TRUE.equals(acquired)) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_LOCK_FAILED);
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
