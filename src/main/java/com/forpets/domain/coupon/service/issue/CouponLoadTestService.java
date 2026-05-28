package com.forpets.domain.coupon.service.issue;

import com.forpets.domain.coupon.dto.CouponLoadTestSummaryResponse;
import com.forpets.domain.coupon.dto.IssueCouponResponse;
import com.forpets.domain.coupon.entity.Coupon;
import com.forpets.domain.coupon.repository.UserCouponRepository;
import com.forpets.domain.coupon.service.CouponService;
import com.forpets.global.exception.BusinessException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("loadtest")
@RequiredArgsConstructor
public class CouponLoadTestService {

    private final CouponService couponService;
    private final CouponIssueOptimisticLockService couponIssueOptimisticLockService;
    private final CouponIssueDistributedLockService couponIssueDistributedLockService;
    private final UserCouponRepository userCouponRepository;
    private final MeterRegistry meterRegistry;

    // 쿠폰 발급 요청
    public IssueCouponResponse issue(Long userId, Long couponId, CouponIssueStrategy strategy) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";

        try {
            return issueByStrategy(userId, couponId, strategy); // 쿠폰 발급 + 어떤 락 여부
        } catch (BusinessException e) {
            result = e.getErrorCode().getCode();
            throw e;
        } catch (RuntimeException e) {
            result = "server_error";
            throw e;
        } finally {
            meterRegistry.counter(
                    "coupon.issue.requests",
                    "strategy", strategy.metricName(),
                    "result", result
            ).increment();

            sample.stop(
                    Timer.builder("coupon.issue.duration") //(평균, 최대, 전략별, 실패 요청, 락)
                            .description("Coupon issue load test duration")
                            .tag("strategy", strategy.metricName())
                            .tag("result", result)
                            .register(meterRegistry)
            );
        }
    }

    // 부하 끝난 뒤 요약 조회
    public CouponLoadTestSummaryResponse getSummary(Long couponId) {
        Coupon coupon = couponService.findCouponById(couponId);
        long issuedCount = userCouponRepository.countByCouponId(couponId);

        return CouponLoadTestSummaryResponse.of(coupon, issuedCount);
    }

    // 락 전달 전략
    private IssueCouponResponse issueByStrategy(Long userId, Long couponId, CouponIssueStrategy strategy) {
        return switch (strategy) {
            case NO_LOCK -> couponService.issueCoupon(userId, couponId);
            case PESSIMISTIC -> couponService.issueCouponWithPessimisticLock(userId, couponId);
            case OPTIMISTIC -> couponIssueOptimisticLockService.issue(userId, couponId);
            case DISTRIBUTED -> couponIssueDistributedLockService.issue(userId, couponId);
        };
    }
}
