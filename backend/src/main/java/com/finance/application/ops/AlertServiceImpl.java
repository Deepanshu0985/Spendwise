package com.finance.application.ops;

import com.finance.domain.ai.AiUsageKind;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertServiceImpl implements AlertService {

    private static final int[] SPEND_LEVELS = {50, 80, 100};

    private final AlertNotifier notifier;
    private final Clock clock;
    private final Map<String, Instant> lastSent = new ConcurrentHashMap<>();
    private final Set<String> spendLevelsReported = ConcurrentHashMap.newKeySet();

    public AlertServiceImpl(AlertNotifier notifier, Clock clock) {
        this.notifier = notifier;
        this.clock = clock;
    }

    @Override
    public void raise(String key, String title, String message, Duration cooldown) {
        Instant now = clock.instant();
        boolean[] send = {false};
        lastSent.compute(key, (k, previous) -> {
            if (previous == null || now.isAfter(previous.plus(cooldown))) {
                send[0] = true;
                return now;
            }
            return previous;
        });
        if (send[0]) {
            notifier.send(title, message);
        }
    }

    @Override
    public void aiMonthlyUsage(AiUsageKind kind, LocalDate month, int used, int limit) {
        if (limit <= 0) {
            return;
        }
        int percent = (int) Math.min(1000L, (long) used * 100 / limit);
        for (int level : SPEND_LEVELS) {
            // Each level is reported once per kind per month, however often usage is checked after it is crossed.
            if (percent >= level && spendLevelsReported.add(kind + "|" + month + "|" + level)) {
                notifier.send(
                        "Spendwise: AI usage at " + level + "%",
                        kind.name().toLowerCase() + " has used " + used + " of the " + limit + " allowed this month (" + percent + "%)."
                                + (level >= 100 ? " AI for everyone is now off until next month." : ""));
            }
        }
    }
}
