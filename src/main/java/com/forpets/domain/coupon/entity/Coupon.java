package com.forpets.domain.coupon.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int discountRate;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int remainingQuantity;

    @Builder
    public Coupon(String name, int discountRate, int totalQuantity) {
        this.name = name;
        this.discountRate = discountRate;
        this.totalQuantity = totalQuantity;
        this.remainingQuantity = totalQuantity;
    }

    // 쿠폰 발급 시 잔여 수량을 1개 차감
    public void decreaseRemainingQuantity() {
        if (this.remainingQuantity <= 0) {
            throw new IllegalStateException("잔여 수량이 없습니다.");
        }

        this.remainingQuantity--;
    }
}
