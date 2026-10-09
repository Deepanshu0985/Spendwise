package com.finance.application.statement;

import com.finance.domain.statement.StatementTransaction;

import java.util.List;
import java.util.UUID;

public interface DuplicateDetectionService {

    /**
     * Scores each staged row against the user's already-confirmed transactions and records the outcome on
     * the row (mutating it; the caller persists). A row mapped to its own account (see
     * StatementTransaction#effectiveAccountId) is compared against that account; the rest use statementAccountId. Rows the user already overrode keep their decision.
     */
    void score(UUID userId, UUID statementAccountId, List<StatementTransaction> rows);
}
