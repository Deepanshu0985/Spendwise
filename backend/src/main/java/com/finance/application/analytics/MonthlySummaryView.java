package com.finance.application.analytics;

import com.finance.domain.analytics.CurrencyExclusion;
import com.finance.domain.analytics.PeriodFigures;

public record MonthlySummaryView(String currency, PeriodFigures figures, CurrencyExclusion exclusion) {
}
