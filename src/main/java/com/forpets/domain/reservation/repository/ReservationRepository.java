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

    /*
    UNAVOIDABLE 자동 승인 후보 조회
    조건: status = CANCEL_REQUESTED, cancelCategory = UNAVOIDABLE, cancelRequestedAt <= cutoff
    - cutoff = 실행일 00:00 - 24시간 (즉 "어제 00:00")
      예: 6/11 00:00 실행 -> cutoff = 6/10 00:00 -> cancelRequestedAt 이 6/10 00:00 이전인 요청 자동 승인
      즉 1일 22:00 요청 -> 2일 00:00 cutoff(=1일 00:00) 에는 미해당, 3일 00:00 cutoff(=2일 00:00) 에 해당
     */
    List<Reservation> findAllByStatusAndCancelCategoryAndCancelRequestedAtLessThanEqual(
            ReservationStatus reservationStatus,
            CancelCategory cancelCategory,
            LocalDateTime cutoff);
}
