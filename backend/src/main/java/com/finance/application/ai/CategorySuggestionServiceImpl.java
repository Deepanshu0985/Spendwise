package com.finance.application.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.application.exception.AiUnavailableException;
import com.finance.domain.ai.AiRowInput;
import com.finance.domain.ai.AiSuggestion;
import com.finance.domain.ai.AiSuggestionValidator;
import com.finance.domain.ai.RawSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Deliberately not @Transactional: it runs a few short transactions (through the store and the limiter) with the model
 * call between them, so no database transaction is held open while waiting on the network.
 */
@Service
public class CategorySuggestionServiceImpl implements CategorySuggestionService {

    private static final Logger log = LoggerFactory.getLogger(CategorySuggestionServiceImpl.class);

    private final CategorySuggestionStore store;
    private final AiModelClient modelClient;
    private final AiUsageLimiter limiter;
    private final AiSettings settings;
    private final ObjectMapper objectMapper;

    public CategorySuggestionServiceImpl(
            CategorySuggestionStore store, AiModelClient modelClient, AiUsageLimiter limiter, AiSettings settings, ObjectMapper objectMapper) {
        this.store = store;
        this.modelClient = modelClient;
        this.limiter = limiter;
        this.settings = settings;
        this.objectMapper = objectMapper;
    }

    @Override
    public CategorySuggestionResult suggest(UUID userId, UUID statementId) {
        boolean aiAvailable = modelClient.isAvailable();
        int budget = aiAvailable ? Math.min(settings.maxRowsPerRequest(), limiter.remainingRows(userId)) : 0;
        CategorySuggestionPlan plan = store.prepare(userId, statementId, aiAvailable, budget);

        int aiApplied = 0;
        int asked = 0;
        String stoppedReason = aiAvailable ? null : "UNAVAILABLE";
        String limitMessage = null;

        List<AiRowInput> toAsk = plan.rowsToAsk();
        for (int from = 0; from < toAsk.size() && stoppedReason == null; from += settings.batchSize()) {
            List<AiRowInput> batch = toAsk.subList(from, Math.min(toAsk.size(), from + settings.batchSize()));
            if (!limiter.tryReserve(userId, batch.size())) {
                stoppedReason = "LIMIT";
                break;
            }
            List<AiSuggestion> suggestions;
            try {
                suggestions = askModel(batch, plan);
            } catch (AiUnavailableException e) {
                limiter.release(userId, batch.size());
                stoppedReason = "UNAVAILABLE";
                break;
            }
            asked += batch.size();
            aiApplied += store.apply(userId, statementId, suggestions, settings.confidenceThreshold());
        }

        int notAsked = plan.eligibleBeyondLimit() + Math.max(0, toAsk.size() - asked);
        if (aiAvailable && stoppedReason == null && plan.eligibleBeyondLimit() > 0 && limiter.remainingRows(userId) == 0) {
            stoppedReason = "LIMIT"; // nothing could be sent at all because a cap was already used up
        }
        if ("LIMIT".equals(stoppedReason)) {
            limitMessage = limiter.limitMessage(userId);
        }
        // Rows beyond the per-request cap are simply "not asked yet": the user can press the button again.
        int needsReview = Math.max(0, asked - aiApplied);
        return new CategorySuggestionResult(
                store.rows(userId, statementId), plan.ruleApplied(), aiApplied, needsReview, notAsked, aiAvailable, stoppedReason, limitMessage);
    }

    @Override
    public AiStatus status(UUID userId) {
        boolean enabled = modelClient.isAvailable();
        return new AiStatus(enabled, enabled ? limiter.remainingRows(userId) : 0, settings.dailyRowLimitPerUser());
    }

    private List<AiSuggestion> askModel(List<AiRowInput> batch, CategorySuggestionPlan plan) {
        String delimiter = CategorizationPrompt.newDelimiter();
        ModelResult result = modelClient.complete(
                CategorizationPrompt.systemPrompt(plan.categories(), delimiter), CategorizationPrompt.userContent(batch, delimiter));
        List<RawSuggestion> raw = CategorizationPrompt.parse(objectMapper, result.text());
        List<AiSuggestion> valid = AiSuggestionValidator.validate(batch, raw, plan.categories());
        // Counts only - never the text of a statement row or of the model's answer.
        log.info("AI categorisation batch: asked={} answered={} accepted={} inputTokens={} outputTokens={}",
                batch.size(), raw.size(), valid.size(), result.inputTokens(), result.outputTokens());
        return new ArrayList<>(valid);
    }
}
