package com.forpets.domain.payment.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfirmPaymentRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("[성공] PortOne V2 paymentId 필드도 merchantUid로 매핑")
    void confirm_payment_request_accepts_payment_id_alias() throws Exception {
        // given
        String json = """
                {
                  "paymentId": "PAY-1-GUARDIAN-20260527120000-A1B2"
                }
                """;

        // when
        ConfirmPaymentRequest result = objectMapper.readValue(json, ConfirmPaymentRequest.class);

        // then
        assertThat(result.merchantUid()).isEqualTo("PAY-1-GUARDIAN-20260527120000-A1B2");
    }
}
