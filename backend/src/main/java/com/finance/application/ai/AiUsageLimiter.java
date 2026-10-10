package com.finance.application.ai;

import com.finance.domain.ai.AiUsageKind;

import java.time.LocalDate;
import java.util.UUID;

public interface AiUsageLimiter {

    /** Units the user may still use today for this kind of AI use, taking the global monthly ceiling into account too. */
    int remaining(UUID userId, AiUsageKind kind);

    /** Reserves units against both caps; false (and nothing reserved) if either would be exceeded. */
    boolean tryReserve(UUID userId, AiUsageKind kind, int units);

    /** Gives reserved units back, for a call that failed before the model produced anything. */
    void release(UUID userId, AiUsageKind kind, int units);

    /** Which cap is the binding one right now, in words fit for a user (with when it resets), or null if none is. */
    String limitMessage(UUID userId, AiUsageKind kind);

    /** The daily allowance for this kind, for showing "x left today". */
    int dailyLimit(AiUsageKind kind);

    /** The day the caps count against (today in the server's clock). */
    LocalDate today();
}
