package com.forpets.domain.coupon.service.issue;

import com.forpets.domain.coupon.dto.IssueCouponResponse;
import com.forpets.domain.coupon.exception.CouponErrorCode;
import com.forpets.domain.coupon.exception.CouponException;
import com.forpets.domain.coupon.service.CouponService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.hibernate.StaleObjectStateException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponIssueOptimisticLockService {

    // 낙관적 락 충돌이 반복될 때 최대 재시도 횟수
    private static final int MAX_RETRY_COUNT = 100;

    // 충돌 직후 같은 시점에 다시 몰리는 것을 줄이기 위한 짧은 대기 시간
    private static final long RETRY_BACKOFF_MILLIS = 5L;

    private final CouponService couponService;

    // 낙관적 락 충돌이 발생하면 새로운 트랜잭션으로 쿠폰 발급을 재시도
    public IssueCouponResponse issue(Long userId, Long couponId) {
        RuntimeException lastOptimisticLockException = null;

        // 각 시도마다 CouponService의 @Transactional 메서드를 다시 호출해 새 트랜잭션으로 처리
        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                return couponService.issueCoupon(userId, couponId);
            } catch (RuntimeException e) {
                // 낙관적 락 충돌이 아닌 예외는 재시도 대상이 아니므로 즉시 전파
                if (!isOptimisticLockException(e)) {
                    throw e;
                }

                // 낙관적 락 충돌이면 잠시 대기 후 다시 조회부터 재시도
                lastOptimisticLockException = e;
                sleepBeforeRetry();
            }
        }

        // 최대 재시도 횟수를 넘기면 마지막 낙관적 락 예외를 그대로 전파
        throw lastOptimisticLockException;
    }

    // 재시도 전 짧게 대기
    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_BACKOFF_MILLIS);
        } catch (InterruptedException e) {
            // 인터럽트 상태를 복구하고 쿠폰 발급 락 실패 예외로 변환
            Thread.currentThread().interrupt();
            throw new CouponException(CouponErrorCode.COUPON_ISSUE_LOCK_FAILED);
        }
    }

    // JPA / Spring / Hibernate에서 감싸져 올라오는 낙관적 락 예외 여부 확인
    private boolean isOptimisticLockException(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof OptimisticLockException
                    || throwable instanceof OptimisticLockingFailureException
                    || throwable instanceof StaleObjectStateException) {
                return true;
            }

            throwable = throwable.getCause();
        }

        return false;
    }
}
