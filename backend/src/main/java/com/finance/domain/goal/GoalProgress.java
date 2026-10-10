package com.finance.domain.goal;

import java.math.BigDecimal;

/**
 * remaining is never negative. requiredPerMonth is how much to set aside each month from now to reach the target by
 * its date; null when there is no target date, the goal is done, or the date has passed (then overdue is true).
 */
public record GoalProgress(BigDecimal remaining, BigDecimal percentComplete, BigDecimal requiredPerMonth, boolean overdue) {
}
