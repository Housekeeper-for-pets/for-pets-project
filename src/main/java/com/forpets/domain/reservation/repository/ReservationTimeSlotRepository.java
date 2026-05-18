package com.forpets.domain.reservation.repository;

import com.forpets.domain.reservation.entity.ReservationTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationTimeSlotRepository extends JpaRepository<ReservationTimeSlot, Long> {

    List<ReservationTimeSlot> findAllByReservationIdOrderByTimeSlotInfoSequence(Long reservationId);
}
