package com.finance.application.analytics;

import java.time.LocalDate;
import java.util.UUID;

public interface AnalyticsService {

    MonthlySummaryView monthlySummary(UUID userId, LocalDate from, LocalDate to, String currency);

    CategoryBreakdownView categoryBreakdown(UUID userId, LocalDate from, LocalDate to, String currency);

    MerchantBreakdownView merchantBreakdown(UUID userId, LocalDate from, LocalDate to, String currency);

    TrendView trend(UUID userId, LocalDate from, LocalDate to, String currency);
}
