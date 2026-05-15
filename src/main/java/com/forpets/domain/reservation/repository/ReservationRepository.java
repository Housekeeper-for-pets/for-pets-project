package com.forpets.domain.reservation.repository;

import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByMemberIdAndStatusIn(Long memberId, List<ReservationStatus> statuses);
}
