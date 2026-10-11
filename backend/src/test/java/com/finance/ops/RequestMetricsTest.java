package com.finance.ops;

import com.finance.infrastructure.ops.RequestMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RequestMetricsTest {

    private static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-10-11T10:00:30Z");

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
    private RequestMetrics metrics;

    @BeforeEach
    void setUp() {
        clock = new MovableClock();
        metrics = new RequestMetrics(clock);
    }

    @Test
    void countsRequestsServerErrorsRefusalsAndFailedSignIns() {
        metrics.record(200, 20, false);
        metrics.record(500, 30, false);
        metrics.record(503, 30, false);
        metrics.record(429, 5, false);
        metrics.record(401, 80, true);
        metrics.record(401, 80, false); // a 401 elsewhere is not a failed sign-in
        RequestMetrics.Snapshot s = metrics.snapshot(5);
        assertThat(s.requests()).isEqualTo(6);
        assertThat(s.serverErrors()).isEqualTo(2);
        assertThat(s.rateLimited()).isEqualTo(1);
        assertThat(s.authFailures()).isEqualTo(1);
    }

    @Test
    void theWindowOnlyCoversTheRequestedMinutes() {
        metrics.record(500, 10, false);
        clock.advance(Duration.ofMinutes(3));
        metrics.record(200, 10, false);
        assertThat(metrics.snapshot(5).requests()).isEqualTo(2);
        assertThat(metrics.snapshot(2).requests()).as("the first is 3 minutes old").isEqualTo(1);
        clock.advance(Duration.ofMinutes(10));
        assertThat(metrics.snapshot(5).requests()).isZero();
    }

    @Test
    void anOldMinuteIsClearedWhenItsSlotIsReusedAnHourLater() {
        metrics.record(500, 10, false);
        clock.advance(Duration.ofMinutes(60));
        metrics.record(200, 10, false);
        RequestMetrics.Snapshot s = metrics.snapshot(5);
        assertThat(s.requests()).isEqualTo(1);
        assertThat(s.serverErrors()).isZero();
    }

    @Test
    void p95IsTheUpperEdgeOfTheBucketHoldingTheNinetyFifthPercentile() {
        for (int i = 0; i < 94; i++) {
            metrics.record(200, 20, false); // < 50ms
        }
        for (int i = 0; i < 6; i++) {
            metrics.record(200, 2000, false); // < 3000ms
        }
        // 94 fast of 100: the 95th request is the first slow one.
        assertThat(metrics.snapshot(5).p95Millis()).isEqualTo(3000);

        MovableClock other = new MovableClock();
        RequestMetrics fast = new RequestMetrics(other);
        for (int i = 0; i < 100; i++) {
            fast.record(200, 20, false);
        }
        assertThat(fast.snapshot(5).p95Millis()).isEqualTo(50);
        assertThat(new RequestMetrics(other).snapshot(5).p95Millis()).isZero();
    }
}
