package com.finance.infrastructure.web.recurring;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Every field optional: only the ones sent are changed. */
public record UpdateRecurringExpenseRequest(@Size(max = 255) String name, UUID categoryId, Boolean confirmed) {
}
