package com.finance.application.insight;

import java.time.YearMonth;
import java.util.UUID;

public interface InsightMetricsBuilder {

    /** Works out the month's figures, comparison with the month before, top categories and merchants, budgets and unusual spending. */
    InsightMetrics build(UUID userId, YearMonth month);
}
