package com.forpets.domain.sitter.repository;

import com.forpets.domain.sitter.entity.SitterSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SitterScheduleRepository extends JpaRepository<SitterSchedule, Long> {
    List<SitterSchedule> findAllBySitterProfileId(Long sitterProfileId);

    List<SitterSchedule> findAllBySitterProfileIdIn(List<Long> sitterProfileIds);

    void deleteAllBySitterProfileId(Long sitterProfileId);
}
