package com.forpets.domain.settlement.repository;

import com.forpets.domain.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findAllByReceiverMemberIdOrderByCreatedAtDesc(Long receiverMemberId);

    boolean existsByReservationId(Long reservationId);
}
