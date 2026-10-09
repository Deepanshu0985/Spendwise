package com.finance.domain.recurring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** One detected repeating payment, before it is stored. */
public record RecurringCandidate(
        String groupKey,
        UUID merchantId,
        UUID categoryId,
        String description,
        String currency,
        BigDecimal averageAmount,
        RecurrenceFrequency frequency,
        LocalDate lastSeenDate,
        LocalDate nextExpectedDate,
        boolean active,
        int occurrences,
        BigDecimal confidence) {
}
