package com.finance.application.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Tunables for AI categorisation. The caps are in rows sent to the model, not money, so they hold whatever the model costs. */
@Component
public class AiSettings {

    private final int dailyRowLimitPerUser;
    private final int monthlyRowLimit;
    private final double confidenceThreshold;
    private final int batchSize;
    private final int maxRowsPerRequest;

    public AiSettings(
            @Value("${ai.limits.daily-rows-per-user:100}") int dailyRowLimitPerUser,
            @Value("${ai.limits.monthly-rows:20000}") int monthlyRowLimit,
            @Value("${ai.categorization.confidence-threshold:0.75}") double confidenceThreshold,
            @Value("${ai.categorization.batch-size:20}") int batchSize,
            @Value("${ai.categorization.max-rows-per-request:60}") int maxRowsPerRequest) {
        this.dailyRowLimitPerUser = dailyRowLimitPerUser;
        this.monthlyRowLimit = monthlyRowLimit;
        this.confidenceThreshold = confidenceThreshold;
        this.batchSize = batchSize;
        this.maxRowsPerRequest = maxRowsPerRequest;
    }

    public int dailyRowLimitPerUser() {
        return dailyRowLimitPerUser;
    }

    public int monthlyRowLimit() {
        return monthlyRowLimit;
    }

    public double confidenceThreshold() {
        return confidenceThreshold;
    }

    public int batchSize() {
        return batchSize;
    }

    public int maxRowsPerRequest() {
        return maxRowsPerRequest;
    }
}
