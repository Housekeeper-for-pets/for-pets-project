package com.forpets.domain.carerequest.repository;

import com.forpets.domain.carerequest.entity.CareRequestTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareRequestTimeSlotRepository extends JpaRepository<CareRequestTimeSlot, Long> {

    List<CareRequestTimeSlot> findAllByCareRequestIdOrderByTimeSlotInfoSequence(Long careRequestId);

    void deleteAllByCareRequestId(Long careRequestId);
}