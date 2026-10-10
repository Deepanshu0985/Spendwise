package com.finance.domain.budget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port - implemented by infrastructure.persistence.budget.BudgetRepositoryImpl. */
public interface BudgetRepository {

    Budget save(Budget budget);

    // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    List<Budget> findByUserId(UUID userId);

    void delete(Budget budget);
}
