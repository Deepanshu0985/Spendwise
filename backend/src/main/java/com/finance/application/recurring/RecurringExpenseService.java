package com.finance.application.recurring;

import com.finance.domain.recurring.RecurringExpense;

import java.util.List;
import java.util.UUID;

public interface RecurringExpenseService {

    /**
     * Re-reads the user's confirmed expenses, stores what repeats and returns the current, non-dismissed list.
     * A pattern the user dismissed stays dismissed; their name, category and confirmed choices are kept.
     */
    List<RecurringExpense> detect(UUID userId);

    /** Stored patterns the user has not dismissed; activeOnly leaves out ones that appear to have stopped. */
    List<RecurringExpense> list(UUID userId, boolean activeOnly);

    RecurringExpense update(UUID userId, UUID id, UpdateRecurringExpenseCommand command);

    /** "This isn't a recurring payment": hides it and keeps it hidden through later detections. Idempotent. */
    void dismiss(UUID userId, UUID id);
}
