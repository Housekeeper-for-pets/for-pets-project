package com.forpets.domain.carerequest.repository;

import com.forpets.domain.carerequest.entity.CareRequest;
import com.forpets.domain.carerequest.entity.CareRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface CareRequestRepository extends JpaRepository<CareRequest, Long> {

    List<CareRequest> findAllByMemberId(Long memberId);

    List<CareRequest> findAllBySitterProfileId(Long sitterProfileId);

    List<CareRequest> findAllBySitterProfileIdAndStatus(Long sitterProfileId, CareRequestStatus status);

    @Query("""
        SELECT CASE WHEN COUNT(cr) > 0 THEN true ELSE false END
        FROM CareRequest cr
        JOIN CareRequestPet crp ON crp.careRequestId = cr.id
        WHERE crp.petId = :petId
        AND cr.status IN :statuses
        """)
    boolean existsByPetIdAndStatusIn(@Param("petId") Long petId, @Param("statuses") List<CareRequestStatus> statuses);


    boolean existsBySitterProfileIdAndStatusIn(Long sitterProfileId, List<CareRequestStatus> statuses);

    /*
    마지막 sequence timeslot 이 지난 PENDING/ACCEPTED 요청 만료 후보 조회
    "미래 timeslot 이 하나도 없음" = 모든 timeslot 이 과거
    timeslot 이 하나라도 있는 요청만 대상 (방어적)
     */
    @Query("""
        SELECT cr FROM CareRequest cr
        WHERE cr.status IN :statuses
          AND EXISTS (SELECT 1 FROM CareRequestTimeSlot ts WHERE ts.careRequestId = cr.id)
          AND NOT EXISTS (
              SELECT 1 FROM CareRequestTimeSlot ts
              WHERE ts.careRequestId = cr.id
                AND (ts.timeSlotInfo.careDate > :today
                     OR (ts.timeSlotInfo.careDate = :today AND ts.timeSlotInfo.endTime > :nowTime))
          )
        """)
    List<CareRequest> findExpireCandidates(@Param("statuses") List<CareRequestStatus> statuses,
                                           @Param("today") LocalDate today,
                                           @Param("nowTime") LocalTime nowTime);
}