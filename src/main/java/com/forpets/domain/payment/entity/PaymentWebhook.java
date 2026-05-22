package com.forpets.domain.payment.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "payment_webhooks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_webhook_event_id", columnNames = "event_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentWebhook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id")
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "merchant_uid", length = 100)
    private String merchantUid;

    @Column(name = "imp_uid", length = 100)
    private String impUid;

    @Column(name = "portone_payment_id", length = 100)
    private String portonePaymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentWebhookStatus status;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public static PaymentWebhook receive(String eventId, String eventType, String merchantUid,
                                         String impUid, String portonePaymentId, String payload) {
        PaymentWebhook webhook = new PaymentWebhook();
        webhook.provider = PaymentProvider.PORTONE;
        webhook.eventId = eventId;
        webhook.eventType = eventType;
        webhook.merchantUid = merchantUid;
        webhook.impUid = impUid;
        webhook.portonePaymentId = portonePaymentId;
        webhook.status = PaymentWebhookStatus.RECEIVED;
        webhook.payload = payload;
        webhook.receivedAt = LocalDateTime.now();
        return webhook;
    }

    public void connectPayment(Long paymentId) {
        this.paymentId = paymentId;
    }

    public void markProcessed() {
        this.status = PaymentWebhookStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    public void markIgnored(String reason) {
        this.status = PaymentWebhookStatus.IGNORED;
        this.failureReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = PaymentWebhookStatus.FAILED;
        this.failureReason = reason;
        this.processedAt = LocalDateTime.now();
    }
}
