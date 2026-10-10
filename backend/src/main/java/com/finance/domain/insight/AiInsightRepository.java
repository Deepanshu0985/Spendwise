package com.finance.domain.insight;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Port - implemented by infrastructure.persistence.insight.AiInsightRepositoryImpl. */
public interface AiInsightRepository {

    AiInsight save(AiInsight insight);

    // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
    Optional<AiInsight> findByUserIdAndTypeAndPeriodStart(UUID userId, InsightType type, LocalDate periodStart);
}
