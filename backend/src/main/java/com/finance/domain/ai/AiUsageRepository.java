package com.finance.domain.ai;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Port - implemented by infrastructure.persistence.ai.AiUsageRepositoryImpl. Reservations are atomic in the database, so
 * two requests at once can never both slip under a cap.
 */
public interface AiUsageRepository {

    /** Adds units to the user's count for the day only if the total stays within limit. */
    boolean tryReserveDaily(UUID userId, AiUsageKind kind, LocalDate day, int units, int limit);

    /** Adds units to the month's global count only if the total stays within limit. */
    boolean tryReserveMonthly(AiUsageKind kind, LocalDate firstOfMonth, int units, int limit);

    void releaseDaily(UUID userId, AiUsageKind kind, LocalDate day, int units);

    void releaseMonthly(AiUsageKind kind, LocalDate firstOfMonth, int units);

    int usedToday(UUID userId, AiUsageKind kind, LocalDate day);

    int usedThisMonth(AiUsageKind kind, LocalDate firstOfMonth);
}
