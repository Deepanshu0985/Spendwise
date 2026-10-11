package com.finance.infrastructure.ops;

import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Counts API requests per minute for the last hour, in memory: how many, how many server errors, how many were turned away
 * by a rate limit, how many sign-ins failed, and a coarse latency histogram. Nothing about who made a request is kept.
 */
@Component
public class RequestMetrics {

    public record Snapshot(int requests, int serverErrors, int rateLimited, int authFailures, long p95Millis) {
    }

    private static final int MINUTES = 60;
    private static final long[] LATENCY_BOUNDS_MS = {50, 100, 250, 500, 1000, 3000, 10_000};

    private final Clock clock;
    private final long[] minuteAt = new long[MINUTES];
    private final int[] requests = new int[MINUTES];
    private final int[] serverErrors = new int[MINUTES];
    private final int[] rateLimited = new int[MINUTES];
    private final int[] authFailures = new int[MINUTES];
    private final int[][] latency = new int[MINUTES][LATENCY_BOUNDS_MS.length + 1];

    public RequestMetrics(Clock clock) {
        this.clock = clock;
    }

    public synchronized void record(int status, long millis, boolean isSignInAttempt) {
        int slot = slotForNow();
        requests[slot]++;
        if (status >= 500) {
            serverErrors[slot]++;
        }
        if (status == 429) {
            rateLimited[slot]++;
        }
        if (isSignInAttempt && status == 401) {
            authFailures[slot]++;
        }
        latency[slot][bucketFor(millis)]++;
    }

    /** Totals over the last {@code minutes} minutes (at most 60), the current one included. */
    public synchronized Snapshot snapshot(int minutes) {
        long nowMinute = clock.millis() / 60_000;
        int totalRequests = 0;
        int totalErrors = 0;
        int totalLimited = 0;
        int totalAuth = 0;
        int[] histogram = new int[LATENCY_BOUNDS_MS.length + 1];
        for (int slot = 0; slot < MINUTES; slot++) {
            if (minuteAt[slot] > nowMinute - minutes && minuteAt[slot] <= nowMinute) {
                totalRequests += requests[slot];
                totalErrors += serverErrors[slot];
                totalLimited += rateLimited[slot];
                totalAuth += authFailures[slot];
                for (int b = 0; b < histogram.length; b++) {
                    histogram[b] += latency[slot][b];
                }
            }
        }
        return new Snapshot(totalRequests, totalErrors, totalLimited, totalAuth, p95(histogram, totalRequests));
    }

    private int slotForNow() {
        long nowMinute = clock.millis() / 60_000;
        int slot = (int) (nowMinute % MINUTES);
        if (minuteAt[slot] != nowMinute) {
            minuteAt[slot] = nowMinute;
            requests[slot] = 0;
            serverErrors[slot] = 0;
            rateLimited[slot] = 0;
            authFailures[slot] = 0;
            java.util.Arrays.fill(latency[slot], 0);
        }
        return slot;
    }

    private static int bucketFor(long millis) {
        for (int i = 0; i < LATENCY_BOUNDS_MS.length; i++) {
            if (millis < LATENCY_BOUNDS_MS[i]) {
                return i;
            }
        }
        return LATENCY_BOUNDS_MS.length;
    }

    /** The upper edge of the bucket holding the 95th percentile (an over-estimate by design; the last bucket reports its lower edge). */
    private static long p95(int[] histogram, int total) {
        if (total == 0) {
            return 0;
        }
        long target = (long) Math.ceil(total * 0.95);
        long seen = 0;
        for (int b = 0; b < histogram.length; b++) {
            seen += histogram[b];
            if (seen >= target) {
                return b < LATENCY_BOUNDS_MS.length ? LATENCY_BOUNDS_MS[b] : LATENCY_BOUNDS_MS[LATENCY_BOUNDS_MS.length - 1];
            }
        }
        return 0;
    }
}
