package com.forpets.domain.reservation.service;

import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Slf4j
@Service
public class ReservationLockService {
    private static final String CONFIRM_LOCK_PREFIX = "lock:reservation:confirm:";
    private static final String RESERVATION_LOCK_PREFIX = "lock:reservation:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate stringRedisTemplate;

    public <T> T executeWithSitterLock(Long sitterProfileId, Supplier<T> task) {
        String lockKey = CONFIRM_LOCK_PREFIX + sitterProfileId;
        String lockValue = UUID.randomUUID().toString();

        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TTL);

        if (!Boolean.TRUE.equals(acquired)) {
            throw new ReservationException(ReservationErrorCode.RESERVATION_CONFIRM_LOCK_FAILED);
        }

        try {
            return task.get();
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    public <T> T executeWithReservationLock(Long reservationId, Supplier<T> task) {
        String lockKey = RESERVATION_LOCK_PREFIX + reservationId;
        String lockValue = UUID.randomUUID().toString();

        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TTL);

        if (!Boolean.TRUE.equals(acquired)) {
            throw new ReservationException(ReservationErrorCode.RESERVATION_LOCK_FAILED);
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
