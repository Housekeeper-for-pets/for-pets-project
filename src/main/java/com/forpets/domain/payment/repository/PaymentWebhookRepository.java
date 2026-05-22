package com.forpets.domain.payment.repository;

import com.forpets.domain.payment.entity.PaymentWebhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentWebhookRepository extends JpaRepository<PaymentWebhook, Long> {

    Optional<PaymentWebhook> findByEventId(String eventId);
}
