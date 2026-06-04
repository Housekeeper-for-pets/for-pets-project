package com.forpets.domain.ai.usage.repository;

import com.forpets.domain.ai.usage.entity.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {
}
