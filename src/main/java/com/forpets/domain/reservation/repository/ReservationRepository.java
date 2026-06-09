package com.forpets.domain.reservation.repository;

import com.forpets.domain.reservation.entity.CancelCategory;
import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findAllByGuardianId(Long guardianId);

    List<Reservation> findAllBySitterMemberId(Long sitterMemberId);

    List<Reservation> findAllBySitterMemberIdOrGuardianId(Long sitterMemberId, Long guardianId);

    List<Reservation> findAllBySitterProfileIdAndStatus(Long sitterProfileId, ReservationStatus status);

    List<Reservation> findAllByStatusAndCreatedAtBefore(ReservationStatus status, LocalDateTime createdAt);

    boolean existsBySitterProfileIdAndStatus(Long sitterProfileId, ReservationStatus status);

    boolean existsByGuardianIdAndStatusIn(Long guardianId, List<ReservationStatus> statuses);

    @Query("""
    SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
    FROM Reservation r
    JOIN ReservationPet rp ON rp.reservationId = r.id
    WHERE rp.petId = :petId
    AND r.status IN :statuses
    """)
    boolean existsByPetIdAndStatusIn(@Param("petId") Long petId, @Param("statuses") List<ReservationStatus> statuses);

    @Query("""
    SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
    FROM Reservation r
    WHERE r.sitterProfileId = :sitterId
    AND r.status IN :statuses
    """)
    boolean existsBySitterIdAndStatusIn(@Param("sitterId") Long sitterId, @Param("statuses") List<ReservationStatus> statuses);


    List<Reservation> findAllByStatusAndCancelCategory(ReservationStatus reservationStatus, CancelCategory cancelCategory);

    Page<Reservation> findAllByStatusAndCancelCategory(ReservationStatus reservationStatus, CancelCategory cancelCategory, Pageable pageable);
}
