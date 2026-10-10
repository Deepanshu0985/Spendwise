package com.finance.infrastructure.persistence.insight;

import com.finance.domain.insight.InsightType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AiInsightJpaRepository extends JpaRepository<AiInsightJpaEntity, UUID> {

    Optional<AiInsightJpaEntity> findByUserIdAndTypeAndPeriodStart(UUID userId, InsightType type, LocalDate periodStart);
}
