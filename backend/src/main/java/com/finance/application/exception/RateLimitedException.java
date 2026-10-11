package com.finance.application.exception;

import org.springframework.http.HttpStatus;

/** Too many requests in a short time. Carries how long to wait so the response can say so (Retry-After). */
public class RateLimitedException extends ApiException {

    private final long retryAfterSeconds;

    public RateLimitedException(long retryAfterSeconds) {
        super(ErrorCode.RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS, message(retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }

    private static String message(long seconds) {
        String wait = seconds < 120 ? seconds + " seconds" : (seconds + 59) / 60 + " minutes";
        return "Too many attempts. Please wait " + wait + " and try again.";
    }
}
