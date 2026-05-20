package com.forpets.domain.reservation.repository;

import com.forpets.domain.reservation.entity.ReservationPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReservationPaymentRepository extends JpaRepository<ReservationPayment, Long> {

    Optional<ReservationPayment> findByReservationId(Long reservationId);
}