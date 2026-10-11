package com.finance.application.ratelimit;

import java.time.Duration;

/**
 * Port: counts attempts per key inside a time window. Keys are made by the caller (rule name plus an IP address, a user id
 * or an email) so one limiter serves every rule. The in-memory adapter is enough for one server instance; a second
 * instance would need a shared store behind this same interface.
 */
public interface RateLimiter {

    /** Counts this attempt and says whether it may go ahead. Use for "at most N requests per window". */
    RateDecision tryAcquire(String key, int limit, Duration window);

    /** Says whether the key has already used up its limit, without counting. Use together with {@link #hit} and {@link #reset}. */
    RateDecision check(String key, int limit);

    /** Counts one attempt (for example a failed login) without deciding anything. */
    void hit(String key, Duration window);

    /** Forgets the key (for example after a successful login). */
    void reset(String key);
}
