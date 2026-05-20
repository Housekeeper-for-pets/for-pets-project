package com.forpets.domain.reservation.repository;

import com.forpets.domain.reservation.entity.ReservationPet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationPetRepository extends JpaRepository<ReservationPet, Long> {

    List<ReservationPet> findAllByReservationId(Long reservationId);
}