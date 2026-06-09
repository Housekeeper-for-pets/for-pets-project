package com.forpets.domain.carelog.repository;

import com.forpets.domain.carelog.entity.CareLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CareLogRepository extends JpaRepository<CareLog, Long> {

    List<CareLog> findByReservationIdOrderByCreatedAtDesc(Long reservationId);

    boolean existsByReservationId(Long reservationId);
}
