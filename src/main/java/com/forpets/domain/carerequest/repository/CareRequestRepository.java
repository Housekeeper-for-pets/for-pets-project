package com.forpets.domain.carerequest.repository;

import com.forpets.domain.carerequest.entity.CareRequest;
import com.forpets.domain.carerequest.entity.CareRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareRequestRepository extends JpaRepository<CareRequest, Long> {

    List<CareRequest> findAllByMemberId(Long memberId);

    List<CareRequest> findAllBySitterProfileId(Long sitterProfileId);

    List<CareRequest> findAllBySitterProfileIdAndStatus(Long sitterProfileId, CareRequestStatus status);
}