package com.finance.application.insight;

import com.finance.domain.insight.AiInsight;

import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

/** The database side of insights, separate from the orchestrator so no transaction is open while a model is being called. */
public interface InsightStore {

    Optional<AiInsight> find(UUID userId, YearMonth month);

    AiInsight upsert(UUID userId, YearMonth month, InsightText text, String supportingData, String metricsHash, String modelName, String promptVersion);
}
