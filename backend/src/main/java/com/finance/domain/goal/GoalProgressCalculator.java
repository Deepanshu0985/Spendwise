package com.finance.domain.goal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class GoalProgressCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private GoalProgressCalculator() {
    }

    public static GoalProgress calculate(Goal goal, LocalDate today) {
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
        BigDecimal percent = goal.getCurrentAmount().multiply(HUNDRED)
                .divide(goal.getTargetAmount(), 1, RoundingMode.HALF_UP).min(HUNDRED);
        if (remaining.signum() == 0 || goal.getTargetDate() == null) {
            return new GoalProgress(remaining, percent, null, false);
        }
        if (!goal.getTargetDate().isAfter(today)) {
            return new GoalProgress(remaining, percent, null, true);
        }
        // Whole months left, at least one: setting aside the remainder over the time actually available.
        long months = Math.max(1, ChronoUnit.MONTHS.between(today, goal.getTargetDate()));
        return new GoalProgress(remaining, percent, remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.CEILING), false);
    }
}
