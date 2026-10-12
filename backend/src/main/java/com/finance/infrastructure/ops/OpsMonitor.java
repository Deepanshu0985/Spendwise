package com.finance.infrastructure.ops;

import com.finance.application.ops.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Looks at the last five minutes of traffic every minute. It reads memory only - never the database - so it cannot keep a
 * sleeping database awake. A sustained run of server errors, or a burst of rate-limit refusals (someone hammering the API),
 * raises an alert; every five minutes with traffic it also writes one metrics line to the log.
 */
@Component
public class OpsMonitor {

    private static final Logger log = LoggerFactory.getLogger(OpsMonitor.class);
    private static final int WINDOW_MINUTES = 5;
    private static final Duration COOLDOWN = Duration.ofHours(1);

    private final RequestMetrics metrics;
    private final AlertService alerts;
    private final int serverErrorMinimum;
    private final int serverErrorPercent;
    private final int rateLimitedMinimum;
    private int ticks;

    public OpsMonitor(
            RequestMetrics metrics,
            AlertService alerts,
            @Value("${ops.alert.server-errors-min:5}") int serverErrorMinimum,
            @Value("${ops.alert.server-errors-percent:20}") int serverErrorPercent,
            @Value("${ops.alert.rate-limited-min:100}") int rateLimitedMinimum) {
        this.metrics = metrics;
        this.alerts = alerts;
        this.serverErrorMinimum = serverErrorMinimum;
        this.serverErrorPercent = serverErrorPercent;
        this.rateLimitedMinimum = rateLimitedMinimum;
    }

    @Scheduled(fixedRate = 60_000, initialDelay = 60_000)
    public void check() {
        evaluate(metrics.snapshot(WINDOW_MINUTES));
        if (++ticks % WINDOW_MINUTES == 0) {
            RequestMetrics.Snapshot s = metrics.snapshot(WINDOW_MINUTES);
            if (s.requests() > 0) {
                log.info("ops-metrics window={}m requests={} serverErrors={} rateLimited={} signInFailures={} p95Ms<={}",
                        WINDOW_MINUTES, s.requests(), s.serverErrors(), s.rateLimited(), s.authFailures(), s.p95Millis());
            }
        }
    }

    public void evaluate(RequestMetrics.Snapshot s) {
        if (s.serverErrors() >= serverErrorMinimum && s.serverErrors() * 100 >= (long) s.requests() * serverErrorPercent) {
            alerts.raise("server-errors", "Spendwise: server errors",
                    s.serverErrors() + " of " + s.requests() + " API requests failed with a server error in the last "
                            + WINDOW_MINUTES + " minutes.", COOLDOWN);
        }
        if (s.rateLimited() >= rateLimitedMinimum) {
            alerts.raise("possible-abuse", "Spendwise: many requests refused",
                    s.rateLimited() + " requests were turned away by a rate limit in the last " + WINDOW_MINUTES
                            + " minutes - someone may be hammering the API.", COOLDOWN);
        }
    }
}
