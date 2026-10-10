package com.finance.domain.recurring;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port - implemented by infrastructure.persistence.recurring.RecurringExpenseRepositoryImpl. */
public interface RecurringExpenseRepository {

    RecurringExpense save(RecurringExpense recurringExpense);

    List<RecurringExpense> saveAll(List<RecurringExpense> recurringExpenses);

    // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
    Optional<RecurringExpense> findByIdAndUserId(UUID id, UUID userId);

    /** Every stored pattern for the user, dismissed ones included (detection needs them to respect the dismissal). */
    List<RecurringExpense> findAllByUserId(UUID userId);

    /**
     * Blocks until no other detection for this user is running, for the rest of the current transaction. Two overlapping
     * detections (a double-click, or the page opening twice) would otherwise both insert the same new pattern.
     */
    void lockDetectionFor(UUID userId);
}
