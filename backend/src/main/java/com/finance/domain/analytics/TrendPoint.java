package com.finance.domain.analytics;

import java.time.YearMonth;

public record TrendPoint(YearMonth month, PeriodFigures figures) {
}
