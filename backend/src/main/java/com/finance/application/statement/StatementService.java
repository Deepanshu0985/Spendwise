package com.finance.application.statement;

import com.finance.domain.statement.Statement;
import com.finance.domain.statement.StatementTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface StatementService {

    Statement upload(UUID userId, UploadStatementCommand command);

    Page<Statement> list(UUID userId, Pageable pageable);

    Statement getOwned(UUID userId, UUID statementId);

    List<StatementTransaction> getTransactions(UUID userId, UUID statementId);

    StatementTransaction updateStagedTransaction(
            UUID userId, UUID statementId, UUID stagingId, UpdateStagedTransactionCommand command);

    /** Idempotent: a repeat call against an already-IMPORTED statement returns the original result, never imports twice. */
    ConfirmResult confirm(UUID userId, UUID statementId);

    /** Re-runs extraction/parsing after a FAILED statement. */
    Statement retry(UUID userId, UUID statementId);
}
