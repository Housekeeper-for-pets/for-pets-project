package com.forpets.domain.coupon.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "user_coupons",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_coupon",
                columnNames = {"user_id", "coupon_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserCouponStatus status;

    @Builder
    public UserCoupon(Long userId, Long couponId) {
        this.userId = userId;
        this.couponId = couponId;
        this.status = UserCouponStatus.ACTIVE;
    }

    // 결제에 쿠폰을 적용하면 사용 완료 상태로 변경
    public void markAsUsed() {
        this.status = UserCouponStatus.USED;
    }

    // 결제 취소 등으로 사용 처리된 쿠폰을 다시 사용 가능 상태로 복구
    public void restore() {
        this.status = UserCouponStatus.ACTIVE;
    }

    // 관리자 회수 등으로 더 이상 사용할 수 없는 상태로 변경
    public void revoke() {
        this.status = UserCouponStatus.REVOKED;
    }
}
