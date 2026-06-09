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

import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class CouponIssueOptimisticLockService {

    // 낙관적 락 충돌 발생 시 최대 재시도 횟수
    private static final int MAX_RETRY_COUNT = 10;

    // 재시도 전 대기 시간(ms)
    private static final long RETRY_BACKOFF_MILLIS = 50L;

    // 테스트 결과 출력용 실제 재시도 횟수
    private final AtomicInteger actualRetryCount = new AtomicInteger();

    private final CouponService couponService;

    // 낙관적 락 충돌이 발생하면 새로운 트랜잭션으로 쿠폰 발급을 재시도
    public IssueCouponResponse issue(Long userId, Long couponId) {
        // 각 시도마다 CouponService의 @Transactional 메서드를 다시 호출해 새 트랜잭션으로 처리
        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                return couponService.issueCoupon(userId, couponId);
            } catch (RuntimeException e) {
                // 낙관적 락 충돌이 아닌 예외는 재시도 대상이 아니므로 즉시 전파
                if (!isOptimisticLockException(e)) {
                    throw e;
                }

                // 낙관적 락 충돌이면 실제 재시도 횟수를 기록
                actualRetryCount.incrementAndGet();

                // 마지막 시도에서는 더 이상 대기하지 않고 반복문 종료 후 실패 예외로 변환
                if (attempt < MAX_RETRY_COUNT) {
                    sleepBeforeRetry();
                }
            }
        }

        // 최대 재시도 횟수를 모두 사용해도 성공하지 못하면 비즈니스 예외로 변환
        throw new CouponException(CouponErrorCode.COUPON_ISSUE_LOCK_FAILED);
    }

    // 테스트 시작 전 실제 재시도 횟수 초기화
    public void resetActualRetryCount() {
        actualRetryCount.set(0);
    }

    // 테스트 결과 출력에서 재시도 설정값을 확인하기 위한 getter
    public int getMaxRetryCount() {
        return MAX_RETRY_COUNT;
    }

    // 테스트 결과 출력에서 재시도 대기 시간을 확인하기 위한 getter
    public long getRetryBackoffMillis() {
        return RETRY_BACKOFF_MILLIS;
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