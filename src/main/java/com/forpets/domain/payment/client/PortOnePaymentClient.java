package com.forpets.domain.payment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.payment.config.PortOneProperties;
import com.forpets.domain.payment.exception.PaymentErrorCode;
import com.forpets.domain.payment.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

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
            return new PortOnePaymentResult(
                    readText(root, "id", "paymentId"),
                    readText(root, "status"),
                    readAmount(root),
                    rawResponse
            );
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new PaymentException(PaymentErrorCode.PORTONE_PAYMENT_VERIFY_FAILED);
        } catch (Exception exception) {
            throw new PaymentException(PaymentErrorCode.PORTONE_PAYMENT_VERIFY_FAILED);
        }
    }

    public PortOneCancelResult cancelPayment(String paymentId, Long amount, String reason) {
        validateApiSecret();

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

            return new PortOneCancelResult(rawResponse);
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new PaymentException(PaymentErrorCode.PORTONE_PAYMENT_CANCEL_FAILED);
        } catch (Exception exception) {
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
