package com.forpets.domain.payment.repository;

import com.forpets.domain.payment.entity.Payment;
import com.forpets.domain.payment.entity.PaymentRole;
import com.forpets.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByMerchantUid(String merchantUid);

    Optional<Payment> findByPortonePaymentId(String portonePaymentId);

    List<Payment> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<Payment> findAllByReservationIdAndStatus(Long reservationId, PaymentStatus status);

    List<Payment> findAllByReservationIdAndStatusIn(Long reservationId, List<PaymentStatus> status);

    Optional<Payment> findByReservationIdAndPaymentRoleAndStatus(
            Long reservationId,
            PaymentRole paymentRole,
            PaymentStatus status
    );

    boolean existsByUserCouponIdAndStatusIn(Long userCouponId, Collection<PaymentStatus> statuses);

    boolean existsByReservationIdAndMemberIdAndPaymentRoleAndStatusIn(
            Long reservationId,
            Long memberId,
            PaymentRole paymentRole,
            Collection<PaymentStatus> statuses
    );
}
