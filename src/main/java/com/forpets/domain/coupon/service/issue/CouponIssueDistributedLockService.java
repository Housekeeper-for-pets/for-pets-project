package com.forpets.domain.coupon.service.issue;

import com.forpets.domain.coupon.dto.IssueCouponResponse;
import com.forpets.domain.coupon.exception.CouponErrorCode;
import com.forpets.domain.coupon.exception.CouponException;
import com.forpets.domain.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Profile("!test")
@RequiredArgsConstructor
public class CouponIssueDistributedLockService {

    // 쿠폰 발급 요청을 쿠폰 ID 단위로 직렬화하기 위한 락 키
    private static final String COUPON_ISSUE_LOCK_PREFIX = "lock:coupon:issue:";

    private final RedissonClient redissonClient;
    private final CouponService couponService;

    // Redisson 분산 락으로 쿠폰 발급 로직 보호
    public IssueCouponResponse issue(Long userId, Long couponId) {
        String lockKey = COUPON_ISSUE_LOCK_PREFIX + couponId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = false;

        try {
            // 최대 3초 동안 락 획득 대기, 획득 후 10초가 지나면 자동 해제
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);

            // 락을 얻지 못하면 쿠폰 발급 요청이 몰린 상황으로 판단
            if (!locked) {
                throw new CouponException(CouponErrorCode.COUPON_ISSUE_LOCK_FAILED);
            }

            // 락을 잡은 상태에서 기존 쿠폰 발급 로직 실행
            // 다른 Bean의 @Transactional 메서드 호출로 트랜잭션 프록시 적용
            return couponService.issueCoupon(userId, couponId);
        } catch (InterruptedException e) {
            // 인터럽트 발생 시 현재 스레드의 인터럽트 상태 복구
            Thread.currentThread().interrupt();
            throw new CouponException(CouponErrorCode.COUPON_ISSUE_LOCK_FAILED);
        } finally {
            // 현재 스레드가 락을 가지고 있을 때만 해제
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
