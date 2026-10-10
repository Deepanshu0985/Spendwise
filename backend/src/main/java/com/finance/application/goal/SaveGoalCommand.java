package com.finance.application.goal;

import java.math.BigDecimal;
import java.time.LocalDate;

/** currency is only used on create (a goal's currency never changes). currentAmount defaults to zero when not given. */
public record SaveGoalCommand(String name, BigDecimal targetAmount, BigDecimal currentAmount, LocalDate targetDate, String currency) {
}
