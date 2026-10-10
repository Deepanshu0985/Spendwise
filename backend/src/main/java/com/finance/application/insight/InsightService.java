package com.finance.application.insight;

import com.finance.domain.insight.AiInsight;

import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

public interface InsightService {

    /** The stored insight for the month, if there is one. Never calls a model. */
    Optional<AiInsight> find(UUID userId, YearMonth month);

    /**
     * Returns the month's insight: the stored one while the figures are unchanged (unless refresh), otherwise a new one. The wording
     * is the model's when AI is on, within its caps and passes the figure check, and a fixed template otherwise - never empty.
     */
    InsightView generate(UUID userId, YearMonth month, boolean refresh);
}
