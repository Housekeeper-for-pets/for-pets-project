package com.forpets.domain.payment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.payment.config.PortOneProperties;
import com.forpets.domain.payment.exception.PaymentErrorCode;
import com.forpets.domain.payment.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortOnePaymentClient {
    private static final String PORTONE_API_URL = "https://api.portone.io";

    private final PortOneProperties portOneProperties;
    private final ObjectMapper objectMapper;

    public PortOnePaymentResult getPayment(String paymentId) {
        validateApiSecret();

        try {
            String rawResponse = RestClient.builder()
                    .baseUrl(PORTONE_API_URL)
                    .defaultHeader("Authorization", "PortOne " + portOneProperties.getApiSecret())
                    .build()
                    .get()
                    .uri("/payments/{paymentId}", paymentId)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(rawResponse);
            PortOnePaymentResult result = new PortOnePaymentResult(
                    readText(root, "id", "paymentId"),
                    readText(root, "status"),
                    readAmount(root),
                    rawResponse
            );
            log.info("[PortOnePaymentClient] getPayment 성공 merchantUid={}, status={}, amount={}",
                    paymentId, result.status(), result.totalAmount());
            return result;
        } catch (RestClientException | IllegalArgumentException exception) {
            // 운영에서 환불 stuck 원인 추적 가능하도록 상세 로그를 반드시 남긴다
            log.error("[PortOnePaymentClient] getPayment 실패 (Rest/Parse) merchantUid={}, error={}",
                    paymentId, exception.toString(), exception);
            throw new PaymentException(PaymentErrorCode.PORTONE_PAYMENT_VERIFY_FAILED);
        } catch (Exception exception) {
            log.error("[PortOnePaymentClient] getPayment 실패 (Unknown) merchantUid={}, error={}",
                    paymentId, exception.toString(), exception);
            throw new PaymentException(PaymentErrorCode.PORTONE_PAYMENT_VERIFY_FAILED);
        }
    }

    public PortOneCancelResult cancelPayment(String paymentId, Long amount, String reason) {
        validateApiSecret();
        log.info("[PortOnePaymentClient] cancelPayment 시작 merchantUid={}, amount={}, reason={}",
                paymentId, amount, reason);

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            if (StringUtils.hasText(portOneProperties.getStoreId())) {
                requestBody.put("storeId", portOneProperties.getStoreId());
            }
            requestBody.put("amount", amount);
            requestBody.put("reason", reason);

            String rawResponse = RestClient.builder()
                    .baseUrl(PORTONE_API_URL)
                    .defaultHeader("Authorization", "PortOne " + portOneProperties.getApiSecret())
                    .build()
                    .post()
                    .uri("/payments/{paymentId}/cancel", paymentId)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.info("[PortOnePaymentClient] cancelPayment 성공 merchantUid={}, amount={}, response={}",
                    paymentId, amount, rawResponse);
            return new PortOneCancelResult(rawResponse);
        } catch (RestClientException | IllegalArgumentException exception) {
            // 환불이 안 됐다는 사용자 리포트의 핵심 추적 지점. 절대 swallow 하지 않는다.
            log.error("[PortOnePaymentClient][CRITICAL] cancelPayment 실패 (Rest/Parse) merchantUid={}, amount={}, reason={}, error={}",
                    paymentId, amount, reason, exception.toString(), exception);
            throw new PaymentException(PaymentErrorCode.PORTONE_PAYMENT_CANCEL_FAILED);
        } catch (Exception exception) {
            log.error("[PortOnePaymentClient][CRITICAL] cancelPayment 실패 (Unknown) merchantUid={}, amount={}, reason={}, error={}",
                    paymentId, amount, reason, exception.toString(), exception);
            throw new PaymentException(PaymentErrorCode.PORTONE_PAYMENT_CANCEL_FAILED);
        }
    }

    private void validateApiSecret() {
        if (!StringUtils.hasText(portOneProperties.getApiSecret())) {
            throw new PaymentException(PaymentErrorCode.PORTONE_PAYMENT_VERIFY_FAILED);
        }
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

    private Long readAmount(JsonNode root) {
        JsonNode amount = root.path("amount");

        // PortOne V2 결제 단건 조회 응답은 결제수단/상태에 따라 금액 위치가 달라질 수 있어
        // 서버 검증에 필요한 총 결제 금액 후보를 안전한 순서로 확인한다.
        if (amount.isObject()) {
            return readLong(amount, "total", "paid", "value");
        }

        if (amount.isNumber()) {
            return amount.asLong();
        }

        return readLong(root, "totalAmount", "paidAmount", "finalAmount");
    }

    private Long readLong(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isNumber()) {
                return value.asLong();
            }
        }
        return null;
    }
}
