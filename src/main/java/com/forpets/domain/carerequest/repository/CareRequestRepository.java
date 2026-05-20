package com.forpets.domain.carerequest.repository;

import com.forpets.domain.carerequest.entity.CareRequest;
import com.forpets.domain.carerequest.entity.CareRequestStatus;
import com.forpets.domain.proposal.entity.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}