package com.finance.application.ops;

import com.finance.domain.ai.AiUsageKind;

import java.time.Duration;
import java.time.LocalDate;

/** Decides when an operational condition is worth a message: once per cooldown, and AI spend at 50%, 80% and 100% of a cap. */
public interface AlertService {

    /** Sends the message unless one with the same key went out within the cooldown. */
    void raise(String key, String title, String message, Duration cooldown);

    /** Called whenever AI usage grows; alerts the first time a monthly cap passes 50%, 80% and 100% (observability-monitoring.md). */
    void aiMonthlyUsage(AiUsageKind kind, LocalDate month, int used, int limit);
}
