package com.forpets.domain.carelog.repository;

import com.forpets.domain.carelog.entity.CareLogImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CareLogImageRepository extends JpaRepository<CareLogImage, Long> {

    List<CareLogImage> findByCareLogId(Long careLogId);

    List<CareLogImage> findByCareLogIdIn(List<Long> careLogIds);
}