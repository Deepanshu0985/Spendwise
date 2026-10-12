package com.finance.ops;

import com.finance.application.ops.AlertService;
import com.finance.domain.ai.AiUsageKind;
import com.finance.infrastructure.ops.OpsMonitor;
import com.finance.infrastructure.ops.RequestMetrics;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpsMonitorTest {

    private final List<String> raised = new ArrayList<>();

    private OpsMonitor monitor() {
        AlertService alerts = new AlertService() {
            @Override
            public void raise(String key, String title, String message, Duration cooldown) {
                raised.add(key);
            }

            @Override
            public void aiMonthlyUsage(AiUsageKind kind, LocalDate month, int used, int limit) {
            }
        };
        return new OpsMonitor(new RequestMetrics(Clock.systemUTC()), alerts, 5, 20, 100);
    }

    @Test
    void sustainedServerErrorsRaiseAnAlertButAFewInBusyTrafficDoNot() {
        monitor().evaluate(new RequestMetrics.Snapshot(40, 8, 0, 0, 100));
        assertThat(raised).containsExactly("server-errors");

        raised.clear();
        monitor().evaluate(new RequestMetrics.Snapshot(1000, 8, 0, 0, 100));
        assertThat(raised).as("0.8% of a busy window is not an outage").isEmpty();

        monitor().evaluate(new RequestMetrics.Snapshot(3, 3, 0, 0, 100));
        assertThat(raised).as("three errors is below the minimum").isEmpty();
    }

    @Test
    void manyRateLimitRefusalsRaiseAPossibleAbuseAlert() {
        monitor().evaluate(new RequestMetrics.Snapshot(500, 0, 120, 0, 100));
        assertThat(raised).containsExactly("possible-abuse");
        raised.clear();
        monitor().evaluate(new RequestMetrics.Snapshot(500, 0, 99, 0, 100));
        assertThat(raised).isEmpty();
    }
}
