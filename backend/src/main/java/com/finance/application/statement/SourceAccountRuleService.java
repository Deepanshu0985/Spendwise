package com.finance.application.statement;

import com.finance.domain.statement.StatementTransaction;

import java.util.List;
import java.util.UUID;

public interface SourceAccountRuleService {

    /** Stores which account a statement's source label means, replacing any earlier answer. */
    void remember(UUID userId, String sourceAccountLabel, UUID accountId);

    /**
     * Assigns the remembered account to rows that have a source label and no account assigned yet.
     * Mutates the rows and returns the ones that changed (the caller persists them).
     */
    List<StatementTransaction> applyTo(UUID userId, List<StatementTransaction> rows);
}
