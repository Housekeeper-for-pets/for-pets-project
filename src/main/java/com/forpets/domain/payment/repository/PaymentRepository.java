package com.forpets.domain.payment.repository;

import com.forpets.domain.payment.entity.Payment;
import com.forpets.domain.payment.entity.PaymentRole;
import com.forpets.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByMerchantUid(String merchantUid);

    boolean existsByReservationIdAndMemberIdAndPaymentRoleAndStatusIn(
            Long reservationId,
            Long memberId,
            PaymentRole paymentRole,
            Collection<PaymentStatus> statuses
    );
}
