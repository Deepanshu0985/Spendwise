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
    private final int dailyChatMessagesPerUser;
    private final int monthlyChatMessages;

    public AiSettings(
            @Value("${ai.limits.daily-rows-per-user:100}") int dailyRowLimitPerUser,
            @Value("${ai.limits.monthly-rows:20000}") int monthlyRowLimit,
            @Value("${ai.categorization.confidence-threshold:0.75}") double confidenceThreshold,
            @Value("${ai.categorization.batch-size:20}") int batchSize,
            @Value("${ai.categorization.max-rows-per-request:60}") int maxRowsPerRequest,
            @Value("${ai.limits.daily-chat-messages-per-user:30}") int dailyChatMessagesPerUser,
            @Value("${ai.limits.monthly-chat-messages:5000}") int monthlyChatMessages) {
        this.dailyRowLimitPerUser = dailyRowLimitPerUser;
        this.monthlyRowLimit = monthlyRowLimit;
        this.confidenceThreshold = confidenceThreshold;
        this.batchSize = batchSize;
        this.maxRowsPerRequest = maxRowsPerRequest;
        this.dailyChatMessagesPerUser = dailyChatMessagesPerUser;
        this.monthlyChatMessages = monthlyChatMessages;
    }

    public int dailyChatMessagesPerUser() {
        return dailyChatMessagesPerUser;
    }

    public int monthlyChatMessages() {
        return monthlyChatMessages;
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
