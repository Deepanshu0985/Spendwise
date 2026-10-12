package com.finance.ops;

import com.finance.application.ops.AlertNotifier;
import com.finance.application.ops.AlertServiceImpl;
import com.finance.domain.ai.AiUsageKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlertServiceTest {

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

    private final List<String> sent = new ArrayList<>();
    private MovableClock clock;
    private AlertServiceImpl alerts;

    @BeforeEach
    void setUp() {
        clock = new MovableClock();
        AlertNotifier notifier = (title, message) -> sent.add(title + " | " + message);
        alerts = new AlertServiceImpl(notifier, clock);
    }

    @Test
    void aRaisedAlertIsSentOnceAndThenHeldBackUntilTheCooldownPasses() {
        alerts.raise("errors", "Errors", "five errors", Duration.ofHours(1));
        alerts.raise("errors", "Errors", "six errors", Duration.ofHours(1));
        clock.advance(Duration.ofMinutes(30));
        alerts.raise("errors", "Errors", "seven errors", Duration.ofHours(1));
        assertThat(sent).hasSize(1);

        clock.advance(Duration.ofMinutes(31));
        alerts.raise("errors", "Errors", "eight errors", Duration.ofHours(1));
        assertThat(sent).hasSize(2).last().asString().contains("eight errors");
    }

    @Test
    void differentKeysAreIndependent() {
        alerts.raise("a", "A", "a", Duration.ofHours(1));
        alerts.raise("b", "B", "b", Duration.ofHours(1));
        assertThat(sent).hasSize(2);
    }

    @Test
    void aiUsageAlertsAtFiftyEightyAndAHundredPercentEachOnlyOncePerMonth() {
        LocalDate october = LocalDate.of(2026, 10, 1);
        alerts.aiMonthlyUsage(AiUsageKind.CHAT, october, 10, 100);
        assertThat(sent).isEmpty();

        alerts.aiMonthlyUsage(AiUsageKind.CHAT, october, 50, 100);
        assertThat(sent).hasSize(1).last().asString().contains("50%");
        alerts.aiMonthlyUsage(AiUsageKind.CHAT, october, 60, 100);
        assertThat(sent).as("still the same level").hasSize(1);

        alerts.aiMonthlyUsage(AiUsageKind.CHAT, october, 81, 100);
        assertThat(sent).hasSize(2).last().asString().contains("80%");

        alerts.aiMonthlyUsage(AiUsageKind.CHAT, october, 100, 100);
        assertThat(sent).hasSize(3).last().asString().contains("100%").contains("off until next month");
        alerts.aiMonthlyUsage(AiUsageKind.CHAT, october, 100, 100);
        assertThat(sent).hasSize(3);
    }

    @Test
    void aJumpStraightPastSeveralLevelsReportsEachOfThem() {
        alerts.aiMonthlyUsage(AiUsageKind.INSIGHT, LocalDate.of(2026, 10, 1), 85, 100);
        assertThat(sent).hasSize(2);
    }

    @Test
    void aNewMonthAndAnotherKindStartFresh() {
        alerts.aiMonthlyUsage(AiUsageKind.CHAT, LocalDate.of(2026, 10, 1), 60, 100);
        alerts.aiMonthlyUsage(AiUsageKind.CHAT, LocalDate.of(2026, 11, 1), 60, 100);
        alerts.aiMonthlyUsage(AiUsageKind.INSIGHT, LocalDate.of(2026, 10, 1), 60, 100);
        assertThat(sent).hasSize(3);
    }
}
