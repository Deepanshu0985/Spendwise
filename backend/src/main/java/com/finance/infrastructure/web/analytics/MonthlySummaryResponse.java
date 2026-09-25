package com.finance.infrastructure.web.analytics;

import com.finance.domain.analytics.PeriodFigures;

import java.math.BigDecimal;

public record MonthlySummaryResponse(String currency, BigDecimal expenses, BigDecimal income, BigDecimal savings, BigDecimal savingsRate) {

    public static MonthlySummaryResponse of(String currency, PeriodFigures figures) {
        return new MonthlySummaryResponse(currency, figures.expenses(), figures.income(), figures.savings(), figures.savingsRate());
    }
}
