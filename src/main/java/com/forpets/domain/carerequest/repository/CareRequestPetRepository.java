package com.forpets.domain.carerequest.repository;

import com.forpets.domain.carerequest.entity.CareRequestPet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareRequestPetRepository extends JpaRepository<CareRequestPet, Long> {

    List<CareRequestPet> findAllByCareRequestId(Long careRequestId);

    void deleteAllByCareRequestId(Long careRequestId);
}