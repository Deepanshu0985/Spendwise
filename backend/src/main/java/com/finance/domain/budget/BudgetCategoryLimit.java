package com.finance.domain.budget;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetCategoryLimit(UUID categoryId, BigDecimal limitAmount) {
}
