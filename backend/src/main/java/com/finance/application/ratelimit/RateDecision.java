package com.finance.application.ratelimit;

/** Whether one more attempt is allowed and, when it is not, how many seconds until the window reopens. */
public record RateDecision(boolean allowed, long retryAfterSeconds) {

    public static RateDecision permit() {
        return new RateDecision(true, 0);
    }

    public static RateDecision refuse(long retryAfterSeconds) {
        return new RateDecision(false, Math.max(1, retryAfterSeconds));
    }
}
