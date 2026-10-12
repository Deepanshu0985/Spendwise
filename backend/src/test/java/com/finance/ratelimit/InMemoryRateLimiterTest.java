package com.finance.ratelimit;

import com.finance.application.ratelimit.RateDecision;
import com.finance.infrastructure.ratelimit.InMemoryRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    private static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-10-11T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private MovableClock clock;
    private InMemoryRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MovableClock();
        limiter = new InMemoryRateLimiter(clock);
    }

    @Test
    void allowsUpToTheLimitThenRefusesWithTheTimeLeft() {
        Duration window = Duration.ofMinutes(1);
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("k", 3, window).allowed()).isTrue();
        }
        clock.advance(Duration.ofSeconds(20));
        RateDecision refused = limiter.tryAcquire("k", 3, window);
        assertThat(refused.allowed()).isFalse();
        assertThat(refused.retryAfterSeconds()).isEqualTo(40);
    }

    @Test
    void theWindowReopensAfterItEnds() {
        Duration window = Duration.ofMinutes(1);
        for (int i = 0; i < 4; i++) {
            limiter.tryAcquire("k", 3, window);
        }
        clock.advance(Duration.ofSeconds(61));
        assertThat(limiter.tryAcquire("k", 3, window).allowed()).isTrue();
    }

    @Test
    void keysAreCountedIndependently() {
        Duration window = Duration.ofMinutes(1);
        limiter.tryAcquire("a", 1, window);
        assertThat(limiter.tryAcquire("a", 1, window).allowed()).isFalse();
        assertThat(limiter.tryAcquire("b", 1, window).allowed()).isTrue();
    }

    @Test
    void checkOnlyLooksAndHitCountsWithoutDeciding() {
        Duration window = Duration.ofMinutes(15);
        for (int i = 0; i < 4; i++) {
            limiter.hit("fail", window);
        }
        assertThat(limiter.check("fail", 5).allowed()).as("4 failures of 5 allowed").isTrue();
        assertThat(limiter.check("fail", 5).allowed()).as("checking does not count").isTrue();
        limiter.hit("fail", window);
        RateDecision blocked = limiter.check("fail", 5);
        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(15 * 60);
    }

    @Test
    void resetForgetsTheKey() {
        Duration window = Duration.ofMinutes(15);
        for (int i = 0; i < 5; i++) {
            limiter.hit("fail", window);
        }
        limiter.reset("fail");
        assertThat(limiter.check("fail", 5).allowed()).isTrue();
    }

    @Test
    void evictionDropsOnlyEndedWindows() {
        limiter.hit("short", Duration.ofSeconds(30));
        limiter.hit("long", Duration.ofHours(1));
        clock.advance(Duration.ofMinutes(2));
        limiter.evictExpired();
        assertThat(limiter.check("short", 1).allowed()).isTrue();
        assertThat(limiter.check("long", 1).allowed()).isFalse();
    }
}
