package com.finance.application.ai;

import com.finance.domain.ai.AiUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class AiUsageLimiterImpl implements AiUsageLimiter {

    private final AiUsageRepository usageRepository;
    private final AiSettings settings;
    private final Clock clock;

    public AiUsageLimiterImpl(AiUsageRepository usageRepository, AiSettings settings, Clock clock) {
        this.usageRepository = usageRepository;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public int remainingRows(UUID userId) {
        int dailyLeft = settings.dailyRowLimitPerUser() - usageRepository.usedToday(userId, today());
        int monthlyLeft = settings.monthlyRowLimit() - usageRepository.usedThisMonth(firstOfMonth());
        return Math.max(0, Math.min(dailyLeft, monthlyLeft));
    }

    @Override
    @Transactional
    public boolean tryReserve(UUID userId, int rows) {
        if (rows <= 0) {
            return true;
        }
        if (!usageRepository.tryReserveMonthly(firstOfMonth(), rows, settings.monthlyRowLimit())) {
            return false;
        }
        if (!usageRepository.tryReserveDaily(userId, today(), rows, settings.dailyRowLimitPerUser())) {
            usageRepository.releaseMonthly(firstOfMonth(), rows);
            return false;
        }
        return true;
    }

    @Override
    @Transactional
    public void release(UUID userId, int rows) {
        usageRepository.releaseDaily(userId, today(), rows);
        usageRepository.releaseMonthly(firstOfMonth(), rows);
    }

    @Override
    @Transactional(readOnly = true)
    public String limitMessage(UUID userId) {
        if (settings.dailyRowLimitPerUser() - usageRepository.usedToday(userId, today()) <= 0) {
            return "You've reached today's limit of " + settings.dailyRowLimitPerUser() + " AI-suggested rows. It resets tomorrow.";
        }
        if (settings.monthlyRowLimit() - usageRepository.usedThisMonth(firstOfMonth()) <= 0) {
            return "AI suggestions have reached their monthly limit for everyone. They resume next month.";
        }
        return null;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDate firstOfMonth() {
        return today().withDayOfMonth(1);
    }
}
