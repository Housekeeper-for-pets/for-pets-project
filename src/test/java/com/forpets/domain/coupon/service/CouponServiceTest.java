package com.forpets.domain.coupon.service;

import com.forpets.domain.coupon.dto.IssueCouponResponse;
import com.forpets.domain.coupon.entity.Coupon;
import com.forpets.domain.coupon.entity.UserCoupon;
import com.forpets.domain.coupon.exception.CouponErrorCode;
import com.forpets.domain.coupon.exception.CouponException;
import com.forpets.domain.coupon.repository.CouponRepository;
import com.forpets.domain.coupon.repository.UserCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService 단위 테스트")
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock private CouponRepository couponRepository;
    @Mock private UserCouponRepository userCouponRepository;

    private Coupon coupon;
    private static final Long COUPON_ID = 1L;
    private static final Long USER_ID = 100L;

    @BeforeEach
    void setUp() {
        coupon = Coupon.builder()
                .name("10% 할인 쿠폰")
                .discountRate(10)
                .totalQuantity(100)
                .build();
        ReflectionTestUtils.setField(coupon, "id", COUPON_ID);
    }

    // =============================================
    // issueCoupon
    // =============================================
    @Nested
    @DisplayName("쿠폰 발급 — issueCoupon()")
    class IssueCouponTest {

        @Test
        @DisplayName("[성공] 정상 발급 시 잔여 수량이 1 감소하고 발급 이력을 반환한다")
        void issueCoupon_success() {
            int beforeQuantity = coupon.getRemainingQuantity();

            UserCoupon userCoupon = UserCoupon.builder()
                    .userId(USER_ID)
                    .couponId(COUPON_ID)
                    .build();
            ReflectionTestUtils.setField(userCoupon, "id", 1L);

            given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));
            given(userCouponRepository.existsByUserIdAndCouponId(USER_ID, COUPON_ID)).willReturn(false);
            given(userCouponRepository.save(any(UserCoupon.class))).willReturn(userCoupon);

            IssueCouponResponse response = couponService.issueCoupon(USER_ID, COUPON_ID);

            assertThat(coupon.getRemainingQuantity()).isEqualTo(beforeQuantity - 1);
            assertThat(response).isNotNull();
            then(userCouponRepository).should().save(any(UserCoupon.class));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 쿠폰이면 COUPON_NOT_FOUND 예외를 던진다")
        void issueCoupon_couponNotFound() {
            given(couponRepository.findById(COUPON_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> couponService.issueCoupon(USER_ID, COUPON_ID))
                    .isInstanceOf(CouponException.class)
                    .satisfies(ex -> assertThat(((CouponException) ex).getErrorCode())
                            .isEqualTo(CouponErrorCode.COUPON_NOT_FOUND));
        }

        @Test
        @DisplayName("[실패] 잔여 수량이 0이면 COUPON_QUANTITY_EXHAUSTED 예외를 던진다")
        void issueCoupon_quantityExhausted() {
            // 잔여 수량을 0으로 세팅
            ReflectionTestUtils.setField(coupon, "remainingQuantity", 0);

            given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));

            assertThatThrownBy(() -> couponService.issueCoupon(USER_ID, COUPON_ID))
                    .isInstanceOf(CouponException.class)
                    .satisfies(ex -> assertThat(((CouponException) ex).getErrorCode())
                            .isEqualTo(CouponErrorCode.COUPON_QUANTITY_EXHAUSTED));

            then(userCouponRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("[실패] 이미 발급받은 쿠폰이면 COUPON_ALREADY_ISSUED 예외를 던진다")
        void issueCoupon_alreadyIssued() {
            given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));
            given(userCouponRepository.existsByUserIdAndCouponId(USER_ID, COUPON_ID)).willReturn(true);

            assertThatThrownBy(() -> couponService.issueCoupon(USER_ID, COUPON_ID))
                    .isInstanceOf(CouponException.class)
                    .satisfies(ex -> assertThat(((CouponException) ex).getErrorCode())
                            .isEqualTo(CouponErrorCode.COUPON_ALREADY_ISSUED));

            then(userCouponRepository).should(never()).save(any());
        }
    }
}