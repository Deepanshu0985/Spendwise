package com.finance.infrastructure.ratelimit;

import com.finance.application.ratelimit.RateDecision;
import com.finance.application.ratelimit.RateLimiter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window counters held in memory: cheap, no database round trip on every request, and nothing about a caller is
 * kept beyond its window. They reset when the server restarts (the free host restarts often) and are per instance; both
 * are acceptable for a beta and the {@link RateLimiter} port is where a shared store would plug in later.
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

    private record Window(long endMillis, int count) {
    }

    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public RateDecision tryAcquire(String key, int limit, Duration window) {
        long now = clock.millis();
        Window updated = count(key, window, now);
        return updated.count() <= limit ? RateDecision.permit() : RateDecision.refuse(secondsUntil(updated.endMillis(), now));
    }

    @Override
    public RateDecision check(String key, int limit) {
        long now = clock.millis();
        Window window = windows.get(key);
        if (window != null && now < window.endMillis() && window.count() >= limit) {
            return RateDecision.refuse(secondsUntil(window.endMillis(), now));
        }
        return RateDecision.permit();
    }

    @Override
    public void hit(String key, Duration window) {
        count(key, window, clock.millis());
    }

    @Override
    public void reset(String key) {
        windows.remove(key);
    }

    private Window count(String key, Duration window, long now) {
        return windows.compute(key, (k, existing) ->
                existing == null || now >= existing.endMillis()
                        ? new Window(now + window.toMillis(), 1)
                        : new Window(existing.endMillis(), existing.count() + 1));
    }

    private static long secondsUntil(long endMillis, long now) {
        return (endMillis - now + 999) / 1000;
    }

    /** Drops windows that have ended, so a stream of one-off callers cannot grow the map without bound. */
    @Scheduled(fixedRate = 60_000)
    public void evictExpired() {
        long now = clock.millis();
        windows.entrySet().removeIf(entry -> now >= entry.getValue().endMillis());
    }

    int size() {
        return windows.size();
    }
}
