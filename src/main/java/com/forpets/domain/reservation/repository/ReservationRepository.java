package com.forpets.domain.reservation.repository;

import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findAllByGuardianId(Long guardianId);

    List<Reservation> findAllBySitterMemberId(Long sitterMemberId);

    List<Reservation> findAllBySitterProfileIdAndStatus(Long sitterProfileId, ReservationStatus status);

    boolean existsBySitterProfileIdAndStatus(Long sitterProfileId, ReservationStatus status);

    boolean existsByGuardianIdAndStatusIn(Long guardianId, List<ReservationStatus> statuses);
}
