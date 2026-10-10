package com.finance.domain.ai;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Port - implemented by infrastructure.persistence.ai.AiUsageRepositoryImpl. Reservations are atomic in the database, so
 * two requests at once can never both slip under a cap.
 */
public interface AiUsageRepository {

    /** Adds rows to the user's count for the day only if the total stays within limit. */
    boolean tryReserveDaily(UUID userId, LocalDate day, int rows, int limit);

    /** Adds rows to the month's global count only if the total stays within limit. */
    boolean tryReserveMonthly(LocalDate firstOfMonth, int rows, int limit);

    void releaseDaily(UUID userId, LocalDate day, int rows);

    void releaseMonthly(LocalDate firstOfMonth, int rows);

    int usedToday(UUID userId, LocalDate day);

    int usedThisMonth(LocalDate firstOfMonth);
}
