package com.finance.application.budget;

import com.finance.domain.budget.Budget;
import com.finance.domain.budget.BudgetProgress;

public record BudgetView(Budget budget, BudgetProgress progress) {
}
