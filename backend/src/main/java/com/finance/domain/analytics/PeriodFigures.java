package com.finance.domain.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The core figures for any period (a single report range or one point of a
 * trend series), per analytics-specification.md's Formulas section:
 * {@code expenses = SUM(in_expenses) - SUM(REFUND); income = SUM(in_income);
 * savings = income - expenses; savings_rate = income > 0 ? savings/income*100 : null}.
 * Built only via {@link #of}, so the formula lives in exactly one place rather
 * than being re-derived in SQL and in Java.
 */
public record PeriodFigures(BigDecimal expenses, BigDecimal income, BigDecimal savings, BigDecimal savingsRate) {

    // Same NUMERIC(19,4) precision the money columns use end to end - not an
    // early-rounding shortcut, just the finite scale BigDecimal division needs
    // to represent a non-terminating quotient at all (e.g. 1/3).
    private static final int SCALE = 4;

    public static PeriodFigures of(BigDecimal expenses, BigDecimal income) {
        BigDecimal savings = income.subtract(expenses);
        // null, never zero, when income is zero - an undefined rate, not a zero one.
        BigDecimal savingsRate = income.compareTo(BigDecimal.ZERO) > 0
                ? savings.multiply(BigDecimal.valueOf(100)).divide(income, SCALE, RoundingMode.HALF_UP)
                : null;
        return new PeriodFigures(expenses, income, savings, savingsRate);
    }
}
