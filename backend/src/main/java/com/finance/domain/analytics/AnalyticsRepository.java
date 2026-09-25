package com.finance.domain.analytics;

import com.finance.domain.transaction.TransactionWithSplits;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Port - implemented by infrastructure.persistence.analytics.AnalyticsRepositoryImpl.
 * Deliberately currency-agnostic: it returns every CONFIRMED, resolved-type
 * transaction in the period regardless of currency, so AnalyticsServiceImpl can
 * partition by the requested currency and report the exclusion itself
 * (analytics-specification.md's Currency Scoping) without a second query.
 */
public interface AnalyticsRepository {

    List<TransactionWithSplits> findConfirmedResolvedTransactions(UUID userId, LocalDate from, LocalDate to);

    /** All categories visible to the user (system + custom, active or not) - a transaction may reference one since deactivated. */
    Map<UUID, String> categoryNamesForUser(UUID userId);

    /** All of the user's merchants. */
    Map<UUID, String> merchantNamesForUser(UUID userId);
}
