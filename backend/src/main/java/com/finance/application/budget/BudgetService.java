package com.finance.application.budget;

import java.util.List;
import java.util.UUID;

public interface BudgetService {

    BudgetView create(UUID userId, SaveBudgetCommand command);

    List<BudgetView> list(UUID userId);

    BudgetView getOwned(UUID userId, UUID id);

    BudgetView update(UUID userId, UUID id, SaveBudgetCommand command);

    void delete(UUID userId, UUID id);
}
