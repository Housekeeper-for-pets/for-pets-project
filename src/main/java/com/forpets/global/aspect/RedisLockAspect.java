package com.forpets.global.aspect;

import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/*
 transaction 보다 Lock 획득이 먼저 실행되어야 하기 때문에
 Order 를 HIGHEST_PRECEDENCE 로 설정
 */

@Slf4j
@RequiredArgsConstructor
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RedisLockAspect {

    private static final String LOCK_PREFIX = "lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate stringRedisTemplate;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint,
                       DistributedLock distributedLock) throws Throwable {

        // distributed Lock annotation 을 사용할 때
        // @DistributedLock(key = "'reservation:' + #reservationId") 요런식으로 사용하게 되면
        // Lock Key 가 자동으로 lock:reservation:1 이런식으로 됨
        String lockKey = LOCK_PREFIX+ parseKey(distributedLock.key(), joinPoint);

        // TTL 만료 후 다른 스레드의 락을 잘못 지우는 것을 막는 안전장치
        String lockValue = UUID.randomUUID().toString();

        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_TTL);

        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("[RedisLockAspect] 락 획득 실패 key={}", lockKey);
            throw new ReservationException(ReservationErrorCode.RESERVATION_LOCK_FAILED);
        }

        try {
            return joinPoint.proceed();
        } finally {
            releaseLock(lockKey, lockValue);
        }

    }

    // 락 해제 시 value 값 (UUID) 가 옳게 되어있는지 확인
    private void releaseLock(String lockKey, String lockValue) {
        String saved = stringRedisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(saved)) {
            stringRedisTemplate.delete(lockKey);
        }
    }

    // 메서드 파라미터에서 SpEL 로 동적 추출하는 메서드
    private String parseKey (String keyExpression, ProceedingJoinPoint joinPoint) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        return parser.parseExpression(keyExpression).getValue(context, String.class);
    }
}
