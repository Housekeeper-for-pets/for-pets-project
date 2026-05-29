package com.forpets.domain.coupon.repository;

import com.forpets.domain.coupon.entity.UserCoupon;
import com.forpets.domain.coupon.entity.UserCouponStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    // 쿠폰 자동 차감 시 가장 먼저 발급된 ACTIVE 쿠폰을 조회
    Optional<UserCoupon> findFirstByUserIdAndStatusOrderByCreatedAtAsc(
            Long userId,
            UserCouponStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<UserCoupon> findAllByUserIdAndStatusOrderByCreatedAtAsc(
            Long userId,
            UserCouponStatus status
    );

    // 회원 응답에서 사용할 ACTIVE 쿠폰 수량을 조회
    long countByUserIdAndStatus(Long userId, UserCouponStatus status);

    // 동일 유저의 중복 쿠폰 발급 여부를 확인
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    // 동시성 테스트에서 특정 쿠폰의 실제 발급 개수를 확인
    long countByCouponId(Long couponId);
}
