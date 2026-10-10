package com.finance.application.budget;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetCategoryLimitCommand(UUID categoryId, BigDecimal limitAmount) {
}
