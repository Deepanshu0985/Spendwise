package com.finance.application.insight;

import com.finance.domain.insight.AiInsight;
import com.finance.domain.insight.AiInsightRepository;
import com.finance.domain.insight.InsightType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

@Service
public class InsightStoreImpl implements InsightStore {

    private final AiInsightRepository repository;

    public InsightStoreImpl(AiInsightRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiInsight> find(UUID userId, YearMonth month) {
        return repository.findByUserIdAndTypeAndPeriodStart(userId, InsightType.MONTHLY_SUMMARY, month.atDay(1));
    }

    @Override
    @Transactional
    public AiInsight upsert(UUID userId, YearMonth month, InsightText text, String supportingData, String metricsHash, String modelName, String promptVersion) {
        AiInsight insight = repository.findByUserIdAndTypeAndPeriodStart(userId, InsightType.MONTHLY_SUMMARY, month.atDay(1))
                .orElseGet(() -> new AiInsight(
                        userId, InsightType.MONTHLY_SUMMARY, text.title(), text.summary(), text.highlights(), month.atDay(1), month.atEndOfMonth(),
                        supportingData, metricsHash, modelName, promptVersion));
        insight.rewrite(text.title(), text.summary(), text.highlights(), supportingData, metricsHash, modelName, promptVersion);
        return repository.save(insight);
    }
}
