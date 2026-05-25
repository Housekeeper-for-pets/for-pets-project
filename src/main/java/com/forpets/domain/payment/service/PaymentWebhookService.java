package com.forpets.domain.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.forpets.domain.payment.dto.PortOneWebhookResponse;
import com.forpets.domain.payment.entity.Payment;
import com.forpets.domain.payment.entity.PaymentWebhook;
import com.forpets.domain.payment.entity.PaymentWebhookStatus;
import com.forpets.domain.payment.repository.PaymentRepository;
import com.forpets.domain.payment.repository.PaymentWebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentWebhookService {

    private final PaymentWebhookRepository paymentWebhookRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final PaymentRefundService paymentRefundService;

    /*
    PortOne 이 보낸 Webhook 요청을 받아서 파싱
    우리가 지정한 table 형식으로 저장하기
     */
    @Transactional
    public PortOneWebhookResponse handlePortOneWebhook(JsonNode payload) {
        WebhookPayload webhookPayload = parsePayload(payload);
        // PaymentWebhook 으로 저장 (eventId 기준 중복방지 로직)
        PaymentWebhook webhook = saveReceivedWebhook(webhookPayload, payload.toString());

        if (webhook.getStatus() != PaymentWebhookStatus.RECEIVED) {
            return new PortOneWebhookResponse(webhook.getId(), webhook.getStatus());
        }

        try {
            process(webhook, webhookPayload, payload.toString());
        } catch (Exception exception) {
            webhook.markFailed(exception.getMessage());
            log.warn("[PaymentWebhookService] 웹훅 처리 실패 eventId={}, merchantUid={}",
                    webhook.getEventId(), webhook.getMerchantUid(), exception);
        }

        return new PortOneWebhookResponse(webhook.getId(), webhook.getStatus());
    }

    private PaymentWebhook saveReceivedWebhook(WebhookPayload payload, String rawPayload) {
        Optional<PaymentWebhook> savedWebhook = paymentWebhookRepository.findByEventId(payload.eventId());
        if (savedWebhook.isPresent()) {
            PaymentWebhook webhook = savedWebhook.get();
            if (webhook.getStatus() == PaymentWebhookStatus.RECEIVED) {
                webhook.markIgnored("중복 웹훅 수신");
            }
            return webhook;
        }

        try {
            return paymentWebhookRepository.save(PaymentWebhook.receive(
                    payload.eventId(),
                    payload.eventType(),
                    payload.merchantUid(),
                    payload.impUid(),
                    payload.portonePaymentId(),
                    rawPayload
            ));
        } catch (DataIntegrityViolationException exception) {
            return paymentWebhookRepository.findByEventId(payload.eventId())
                    .orElseThrow(() -> exception);
        }
    }

    /*
    실제 비즈니스 처리
    merchantUid 가 존재하는지 + merchantUid 로 생성된 payment 가 있는지 확인
    webhook 은 받았는데 paymnet 가 없으면 Webhook Status = IGNORED, 존재하지 않는 결제 웹훅으로 이유 저장
     */
    private void process(PaymentWebhook webhook, WebhookPayload payload, String rawPayload) {
        if (!StringUtils.hasText(payload.merchantUid())) {
            webhook.markIgnored("결제 ID가 없는 웹훅");
            return;
        }

        Optional<Payment> payment = paymentRepository.findByMerchantUid(payload.merchantUid())
                .or(() -> paymentRepository.findByPortonePaymentId(payload.portonePaymentId()));

        if (payment.isEmpty()) {
            webhook.markIgnored("존재하지 않는 결제 웹훅");
            return;
        }

        // 결제 table 과 연결
        webhook.connectPayment(payment.get().getId());

        /* 상태값에 따라 분기
        PAID: 결제 확인
        FAILED: 결제 실패 처리
        CANCELED: 환불 동기화
         */
        String status = normalize(payload.status());
        switch (status) {
            case "PAID" -> paymentService.confirmByWebhook(payload.merchantUid());
            case "FAILED" -> paymentService.failByWebhook(payload.merchantUid(), "PortOne 웹훅 결제 실패");
            case "CANCELLED", "CANCELED", "REFUNDED" ->
                    paymentRefundService.syncRefundedByWebhook(payload.merchantUid(), "PortOne 웹훅 환불 동기화", rawPayload);
            default -> {
                webhook.markIgnored("처리 대상이 아닌 결제 상태: " + payload.status());
                return;
            }
        }

        webhook.markProcessed();
    }

    private WebhookPayload parsePayload(JsonNode payload) {
        String merchantUid = readText(payload, "payment_id", "paymentId", "merchant_uid", "merchantUid");
        String status = readText(payload, "status", "eventType", "event_type");
        String eventType = readText(payload, "event_type", "eventType", "status");
        String eventId = readText(payload, "event_id", "eventId", "webhook_id", "webhookId", "tx_id", "txId");
        String impUid = readText(payload, "imp_uid", "impUid");
        String portonePaymentId = readText(payload, "portone_payment_id", "portonePaymentId", "payment_id", "paymentId");

        if (!StringUtils.hasText(eventId)) {
            eventId = UUID.nameUUIDFromBytes(payload.toString().getBytes()).toString();
        }

        return new WebhookPayload(eventId, eventType, status, merchantUid, impUid, portonePaymentId);
    }

    private String readText(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = root.path(fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private record WebhookPayload(
            String eventId,
            String eventType,
            String status,
            String merchantUid,
            String impUid,
            String portonePaymentId
    ) {
    }
}
