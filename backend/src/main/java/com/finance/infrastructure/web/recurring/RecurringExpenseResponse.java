package com.finance.infrastructure.web.recurring;

import com.finance.domain.recurring.RecurrenceFrequency;
import com.finance.domain.recurring.RecurringExpense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecurringExpenseResponse(
        UUID id,
        String name,
        UUID merchantId,
        UUID categoryId,
        String currency,
        BigDecimal averageAmount,
        RecurrenceFrequency frequency,
        LocalDate lastSeenDate,
        LocalDate nextExpectedDate,
        BigDecimal monthlyEstimate,
        BigDecimal yearlyEstimate,
        int occurrences,
        BigDecimal confidenceScore,
        boolean isActive,
        boolean confirmed) {

    public static RecurringExpenseResponse from(RecurringExpense e) {
        return new RecurringExpenseResponse(
                e.getId(), e.getName(), e.getMerchantId(), e.getCategoryId(), e.getCurrency(), e.getAverageAmount(), e.getFrequency(),
                e.getLastSeenDate(), e.getNextExpectedDate(), e.getMonthlyEstimate(), e.getYearlyEstimate(), e.getOccurrences(),
                e.getConfidenceScore(), e.isActive(), e.isConfirmed());
    }
}
