package com.forpets.domain.payment.service;

import com.forpets.domain.payment.entity.PaymentRole;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class PaymentMerchantUidGenerator {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String generate(Long reservationId, PaymentRole paymentRole) {
        String requestedAt = LocalDateTime.now().format(FORMATTER);
        String randomCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "PAY-" + reservationId + "-" + paymentRole.name() + "-" + requestedAt + "-" + randomCode;
    }
}
