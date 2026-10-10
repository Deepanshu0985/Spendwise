package com.finance.application.ai;

import java.util.UUID;

public interface AiUsageLimiter {

    /** Rows the user may still send today, taking the global monthly ceiling into account too. */
    int remainingRows(UUID userId);

    /** Reserves rows against both caps; false (and nothing reserved) if either would be exceeded. */
    boolean tryReserve(UUID userId, int rows);

    /** Gives reserved rows back, for a call that failed before the model produced anything. */
    void release(UUID userId, int rows);

    /** Which cap is the binding one right now, in words fit for a user, for when remainingRows is zero. */
    String limitMessage(UUID userId);
}
