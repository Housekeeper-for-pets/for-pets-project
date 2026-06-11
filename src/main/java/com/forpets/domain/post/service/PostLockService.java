package com.forpets.domain.post.service;

import com.forpets.domain.post.exception.PostErrorCode;
import com.forpets.domain.post.exception.PostException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/*
공고에 대한 Redis 분산락
만료 스케줄러가 공고 상태를 변경하는 동안 동일 공고에 대한 다른 변경 작업이
끼어들지 못하도록 보호한다 (ReservationLockService 패턴 차용)
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class PostLockService {
    private static final String POST_LOCK_PREFIX = "lock:post:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate stringRedisTemplate;

    public <T> T executeWithPostLock(Long postId, Supplier<T> task) {
        String lockKey = POST_LOCK_PREFIX + postId;
        String lockValue = UUID.randomUUID().toString();

        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TTL);

        if (!Boolean.TRUE.equals(acquired)) {
            throw new PostException(PostErrorCode.POST_LOCK_FAILED);
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
