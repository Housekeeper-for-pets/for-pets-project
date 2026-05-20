package com.forpets.domain.coupon.service;

import com.forpets.domain.coupon.dto.CouponResponse;
import com.forpets.domain.coupon.dto.CreateCouponRequest;
import com.forpets.domain.coupon.exception.CouponErrorCode;
import com.forpets.domain.coupon.exception.CouponException;
import com.forpets.domain.coupon.repository.CouponRepository;
import com.forpets.domain.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private static final int FIXED_DISCOUNT_RATE = 10;

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    // 관리자가 쿠폰을 생성할 때 현재 정책의 고정 할인율 10%를 적용합니다.
    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        Coupon coupon = Coupon.builder()
                .name(request.name())
                .discountRate(FIXED_DISCOUNT_RATE)
                .totalQuantity(request.totalQuantity())
                .build();

        return CouponResponse.from(couponRepository.save(coupon));
    }

    // 쿠폰 ID 기준으로 쿠폰을 조회하고, 없으면 쿠폰 도메인 예외로 변환합니다.
    public Coupon findCouponById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponException(CouponErrorCode.COUPON_NOT_FOUND));
    }

    // 유저 쿠폰 ID 기준으로 발급 쿠폰을 조회하고, 없으면 쿠폰 도메인 예외로 변환합니다.
    public UserCoupon findUserCouponById(Long userCouponId) {
        return userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CouponException(CouponErrorCode.COUPON_NOT_FOUND));
    }
}
