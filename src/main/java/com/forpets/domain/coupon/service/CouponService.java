package com.forpets.domain.coupon.service;

import com.forpets.domain.coupon.dto.CouponResponse;
import com.forpets.domain.coupon.dto.CreateCouponRequest;
import com.forpets.domain.coupon.dto.RevokeCouponResponse;
import com.forpets.domain.coupon.entity.Coupon;
import com.forpets.domain.coupon.entity.UserCoupon;
import com.forpets.domain.coupon.entity.UserCouponStatus;
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

    // 쿠폰 생성시 고정 할인율 10%
    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        Coupon coupon = Coupon.builder()
                .name(request.name())
                .discountRate(FIXED_DISCOUNT_RATE)
                .totalQuantity(request.totalQuantity())
                .build();

        return CouponResponse.from(couponRepository.save(coupon));
    }

    // 관리자가 사용 전 쿠폰을 회수 처리
    @Transactional
    public RevokeCouponResponse revokeCoupon(Long userCouponId) {
        UserCoupon userCoupon = findUserCouponById(userCouponId);

        if (userCoupon.getStatus() == UserCouponStatus.USED) {
            throw new CouponException(CouponErrorCode.COUPON_ALREADY_USED);
        }

        userCoupon.revoke();
        return RevokeCouponResponse.from(userCoupon);
    }

    // 쿠폰 ID 조회
    public Coupon findCouponById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponException(CouponErrorCode.COUPON_NOT_FOUND));
    }

    // 유저 쿠폰 ID 조회
    public UserCoupon findUserCouponById(Long userCouponId) {
        return userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CouponException(CouponErrorCode.COUPON_NOT_FOUND));
    }
}
