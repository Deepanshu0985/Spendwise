package com.finance.application.ai;

import com.finance.domain.ai.AiUsageKind;
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
    public int remaining(UUID userId, AiUsageKind kind) {
        int dailyLeft = dailyLimit(kind) - usageRepository.usedToday(userId, kind, today());
        int monthlyLeft = monthlyLimit(kind) - usageRepository.usedThisMonth(kind, firstOfMonth());
        return Math.max(0, Math.min(dailyLeft, monthlyLeft));
    }

    @Override
    @Transactional
    public boolean tryReserve(UUID userId, AiUsageKind kind, int units) {
        if (units <= 0) {
            return true;
        }
        if (!usageRepository.tryReserveMonthly(kind, firstOfMonth(), units, monthlyLimit(kind))) {
            return false;
        }
        if (!usageRepository.tryReserveDaily(userId, kind, today(), units, dailyLimit(kind))) {
            usageRepository.releaseMonthly(kind, firstOfMonth(), units);
            return false;
        }
        return true;
    }

    @Override
    @Transactional
    public void release(UUID userId, AiUsageKind kind, int units) {
        usageRepository.releaseDaily(userId, kind, today(), units);
        usageRepository.releaseMonthly(kind, firstOfMonth(), units);
    }

    @Override
    @Transactional(readOnly = true)
    public String limitMessage(UUID userId, AiUsageKind kind) {
        String what = kind == AiUsageKind.CHAT ? "assistant messages" : "AI-suggested rows";
        if (dailyLimit(kind) - usageRepository.usedToday(userId, kind, today()) <= 0) {
            return "You've reached today's limit of " + dailyLimit(kind) + " " + what + ". It resets tomorrow.";
        }
        if (monthlyLimit(kind) - usageRepository.usedThisMonth(kind, firstOfMonth()) <= 0) {
            return "The monthly limit of " + what + " for everyone has been reached. It resets on the 1st of next month.";
        }
        return null;
    }

    @Override
    public int dailyLimit(AiUsageKind kind) {
        return kind == AiUsageKind.CHAT ? settings.dailyChatMessagesPerUser() : settings.dailyRowLimitPerUser();
    }

    private int monthlyLimit(AiUsageKind kind) {
        return kind == AiUsageKind.CHAT ? settings.monthlyChatMessages() : settings.monthlyRowLimit();
    }

    @Override
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDate firstOfMonth() {
        return today().withDayOfMonth(1);
    }
}
