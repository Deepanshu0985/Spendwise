package com.finance.application.recurring;

import java.util.UUID;

/** Every field optional: only the ones given are changed. */
public record UpdateRecurringExpenseCommand(String name, UUID categoryId, Boolean confirmed) {
}
