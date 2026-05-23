package com.forpets.domain.payment.entity;

import com.forpets.domain.payment.exception.PaymentErrorCode;
import com.forpets.domain.payment.exception.PaymentException;
import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_merchant_uid", columnNames = "merchant_uid"),
        @UniqueConstraint(name = "uk_payment_imp_uid", columnNames = "imp_uid")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_role", nullable = false, length = 20)
    private PaymentRole paymentRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType;

    @Column(name = "original_amount", nullable = false)
    private Long originalAmount;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount;

    @Column(name = "final_amount", nullable = false)
    private Long finalAmount;

    @Column(name = "user_coupon_id")
    private Long userCouponId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(name = "imp_uid")
    private String impUid;

    @Column(name = "merchant_uid", nullable = false, length = 100)
    private String merchantUid;

    @Column(name = "portone_payment_id")
    private String portonePaymentId;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "failed_reason")
    private String failedReason;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Builder
    private Payment(Long reservationId, Long memberId, PaymentRole paymentRole,
                    PaymentType paymentType, Long originalAmount, Long discountAmount,
                    Long finalAmount, Long userCouponId, PaymentProvider provider,
                    String merchantUid) {
        this.reservationId = reservationId;
        this.memberId = memberId;
        this.paymentRole = paymentRole;
        this.paymentType = paymentType;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.userCouponId = userCouponId;
        this.status = PaymentStatus.READY;
        this.provider = provider;
        this.merchantUid = merchantUid;
        this.requestedAt = LocalDateTime.now();
    }

    public boolean isReady() {
        return this.status == PaymentStatus.READY;
    }

    public boolean isConfirmable() {
        return this.status == PaymentStatus.READY || this.status == PaymentStatus.PENDING;
    }

    public boolean isFailable() {
        return this.status == PaymentStatus.READY || this.status == PaymentStatus.PENDING;
    }

    public boolean isPaid() {
        return this.status == PaymentStatus.PAID;
    }

    public void approve(String portonePaymentId, String rawResponse) {
        this.status = PaymentStatus.PAID;
        this.portonePaymentId = portonePaymentId;
        this.rawResponse = rawResponse;
        this.approvedAt = LocalDateTime.now();
    }

    public void fail(String failedReason) {
        this.status = PaymentStatus.FAILED;
        this.failedReason = failedReason;
    }

    public void refund(String cancelReason, String rawResponse) {
        this.status = PaymentStatus.REFUNDED;
        this.cancelReason = cancelReason;
        this.rawResponse = rawResponse;
        this.refundedAt = LocalDateTime.now();
    }

    public void cancel(String cancelReason, String rawResponse) {
        this.status = PaymentStatus.CANCELED;
        this.cancelReason = cancelReason;
        this.rawResponse = rawResponse;
        this.canceledAt = LocalDateTime.now();
    }

    public void expire(){
        if (this.status == PaymentStatus.READY || this.status == PaymentStatus.PENDING){
            this.status = PaymentStatus.EXPIRED;
            return;
        }
        throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS);
    }
}
