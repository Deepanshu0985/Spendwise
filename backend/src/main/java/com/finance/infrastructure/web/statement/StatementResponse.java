package com.finance.infrastructure.web.statement;

import com.finance.domain.statement.Statement;
import com.finance.domain.statement.StatementStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StatementResponse(
        UUID id,
        UUID accountId,
        String fileName,
        String fileType,
        LocalDate periodStart,
        LocalDate periodEnd,
        StatementStatus status,
        String errorMessage,
        Instant createdAt,
        Instant processedAt) {

    public static StatementResponse from(Statement statement) {
        return new StatementResponse(
                statement.getId(),
                statement.getAccountId(),
                statement.getFileName(),
                statement.getFileType(),
                statement.getPeriodStart(),
                statement.getPeriodEnd(),
                statement.getStatus(),
                statement.getErrorMessage(),
                statement.getCreatedAt(),
                statement.getProcessedAt());
    }
}
