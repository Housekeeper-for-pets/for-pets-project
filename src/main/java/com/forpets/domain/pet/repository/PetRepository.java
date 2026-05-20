package com.forpets.domain.pet.repository;

import com.forpets.domain.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findAllByMemberId(Long memberId);

    long countByMemberId(Long memberId);

    
}
