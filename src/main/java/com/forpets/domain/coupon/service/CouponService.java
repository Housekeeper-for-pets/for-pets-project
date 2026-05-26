package com.forpets.domain.coupon.service;

import com.forpets.domain.coupon.dto.CouponResponse;
import com.forpets.domain.coupon.dto.CreateCouponRequest;
import com.forpets.domain.coupon.dto.CouponApplyResult;
import com.forpets.domain.coupon.dto.IssueCouponResponse;
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

import java.util.List;
import java.util.Optional;

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

    // 로그인한 회원에게 쿠폰을 발급하고 잔여 수량을 차감합니다.
    @Transactional
    public IssueCouponResponse issueCoupon(Long userId, Long couponId) {
        Coupon coupon = findCouponById(couponId);

        if (coupon.getRemainingQuantity() <= 0) {
            throw new CouponException(CouponErrorCode.COUPON_QUANTITY_EXHAUSTED);
        }

        if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new CouponException(CouponErrorCode.COUPON_ALREADY_ISSUED);
        }

        coupon.decreaseRemainingQuantity();

        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .couponId(couponId)
                .build();

        return IssueCouponResponse.of(userCouponRepository.save(userCoupon), coupon);
    }

    // 결제 전 쿠폰 유효성 검증과 할인 금액 계산만 수행
    public CouponApplyResult applyCoupon(Long userId, Long userCouponId, Long originalPrice) {
        UserCoupon userCoupon = findUserCouponById(userCouponId);

        validateCouponOwner(userCoupon, userId);
        validateCouponActive(userCoupon);

        Coupon coupon = findCouponById(userCoupon.getCouponId());
        long discountAmount = originalPrice * coupon.getDiscountRate() / 100;
        long finalPrice = originalPrice - discountAmount;

        return new CouponApplyResult(discountAmount, finalPrice, coupon.getName());
    }

    // 결제 도메인에서 자동 적용할 가장 오래된 ACTIVE 쿠폰 ID를 조회
    public Optional<Long> findFirstActiveUserCouponId(Long userId) {
        return userCouponRepository.findFirstByUserIdAndStatusOrderByCreatedAtAsc(
                        userId,
                        UserCouponStatus.ACTIVE
                )
                .map(UserCoupon::getId);
    }

    public List<Long> findActiveUserCouponIds(Long userId) {
        return userCouponRepository.findAllByUserIdAndStatusOrderByCreatedAtAsc(
                        userId,
                        UserCouponStatus.ACTIVE
                )
                .stream()
                .map(UserCoupon::getId)
                .toList();
    }


     // 결제 성공 후 쿠폰 상태를 USED로 변경
    @Transactional
    public void markCouponAsUsed(Long userId, Long userCouponId) {
        UserCoupon userCoupon = findUserCouponById(userCouponId);

        validateCouponOwner(userCoupon, userId);
        validateCouponActive(userCoupon);

        userCoupon.markAsUsed();
    }


     // 결제 실패 / 환불 쿠폰 상태를 ACTIVE로 복원
    @Transactional
    public void restoreCoupon(Long userId, Long userCouponId) {
        UserCoupon userCoupon = findUserCouponById(userCouponId);

        validateCouponOwner(userCoupon, userId);

        userCoupon.restore();
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

    private void validateCouponOwner(UserCoupon userCoupon, Long userId) {
        if (!userCoupon.getUserId().equals(userId)) {
            throw new CouponException(CouponErrorCode.COUPON_NOT_OWNED);
        }
    }

    private void validateCouponActive(UserCoupon userCoupon) {
        if (userCoupon.getStatus() != UserCouponStatus.ACTIVE) {
            throw new CouponException(CouponErrorCode.COUPON_ALREADY_USED);
        }
    }
}
