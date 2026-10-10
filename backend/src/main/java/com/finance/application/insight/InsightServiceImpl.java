package com.finance.application.insight;

import com.finance.application.ai.AiModelClient;
import com.finance.application.ai.AiUsageLimiter;
import com.finance.application.exception.AiUnavailableException;
import com.finance.application.exception.DomainValidationException;
import com.finance.domain.ai.AiUsageKind;
import com.finance.domain.insight.AiInsight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Deliberately not @Transactional: it calls a model between short transactions (the metrics builder and the store each run their own).
 * The figures are computed by the application; the model only puts them into words, and only if it is on, within its caps and its
 * wording passes the figure check. Otherwise the template is used, so an insight always exists and never depends on a provider.
 */
@Service
public class InsightServiceImpl implements InsightService {

    private static final Logger log = LoggerFactory.getLogger(InsightServiceImpl.class);
    private static final int MAX_YEARS_BACK = 5;

    private final InsightMetricsBuilder metricsBuilder;
    private final InsightStore store;
    private final InsightWriter modelWriter;
    private final InsightWriter templateWriter;
    private final AiModelClient modelClient;
    private final AiUsageLimiter limiter;
    private final Clock clock;

    public InsightServiceImpl(
            InsightMetricsBuilder metricsBuilder, InsightStore store, @Qualifier("modelInsightWriter") InsightWriter modelWriter,
            @Qualifier("templateInsightWriter") InsightWriter templateWriter, AiModelClient modelClient, AiUsageLimiter limiter, Clock clock) {
        this.metricsBuilder = metricsBuilder;
        this.store = store;
        this.modelWriter = modelWriter;
        this.templateWriter = templateWriter;
        this.modelClient = modelClient;
        this.limiter = limiter;
        this.clock = clock;
    }

    @Override
    public Optional<AiInsight> find(UUID userId, YearMonth month) {
        requireValidMonth(month);
        return store.find(userId, month);
    }

    @Override
    public InsightView generate(UUID userId, YearMonth month, boolean refresh) {
        requireValidMonth(month);
        InsightMetrics metrics = metricsBuilder.build(userId, month);
        Optional<AiInsight> existing = store.find(userId, month);
        if (existing.isPresent() && existing.get().getMetricsHash().equals(metrics.hash()) && !refresh) {
            return new InsightView(existing.get(), true, null);
        }

        String note = null;
        InsightText text = null;
        String modelName = AiInsight.DETERMINISTIC_MODEL;
        String promptVersion = TemplateInsightWriter.TEMPLATE_VERSION;

        if (metrics.hasActivity()) {
            if (!modelClient.isAvailable()) {
                note = "AI is switched off, so this summary is written from your figures without it.";
            } else if (!limiter.tryReserve(userId, AiUsageKind.INSIGHT, 1)) {
                String why = limiter.limitMessage(userId, AiUsageKind.INSIGHT);
                note = (why != null ? why : "The AI insight limit has been reached.") + " This summary is written from your figures without it.";
            } else {
                try {
                    Optional<InsightText> written = modelWriter.write(metrics.json());
                    if (written.isPresent()) {
                        text = written.get();
                        modelName = modelClient.modelName();
                        promptVersion = ModelInsightWriter.PROMPT_VERSION;
                    } else {
                        note = "The AI's wording could not be checked against your figures, so this summary is written from them directly.";
                    }
                } catch (AiUnavailableException e) {
                    limiter.release(userId, AiUsageKind.INSIGHT, 1); // nothing was delivered, so no insight is used
                    note = "The AI is not responding right now, so this summary is written from your figures without it.";
                }
            }
        }
        if (text == null) {
            text = templateWriter.write(metrics.json()).orElseThrow();
        }
        AiInsight saved = store.upsert(userId, month, text, metrics.json().toString(), metrics.hash(), modelName, promptVersion);
        log.info("Insight stored: byModel={} note={}", saved.writtenByModel(), note != null);
        return new InsightView(saved, false, note);
    }

    private void requireValidMonth(YearMonth month) {
        YearMonth now = YearMonth.now(clock);
        if (month.isAfter(now)) {
            throw new DomainValidationException("An insight can only be written for the current month or an earlier one.", List.of());
        }
        if (month.isBefore(now.minusYears(MAX_YEARS_BACK))) {
            throw new DomainValidationException("That month is too far back; insights cover the last " + MAX_YEARS_BACK + " years.", List.of());
        }
    }
}
