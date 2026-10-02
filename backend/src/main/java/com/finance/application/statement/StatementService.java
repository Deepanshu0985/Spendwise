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

    /**
     * Re-scores a READY_FOR_REVIEW statement's rows against everything imported since they were staged
     * (e.g. another statement confirmed in the meantime). Rows whose duplicate flag the user overrode keep it.
     */
    List<StatementTransaction> recheckDuplicates(UUID userId, UUID statementId);

    /** The user choosing to import a flagged duplicate anyway; recorded as audit metadata on the row. */
    StatementTransaction keepDuplicate(UUID userId, UUID statementId, UUID stagingId);

    /** Idempotent: a repeat call against an already-IMPORTED statement returns the original result, never imports twice. */
    ConfirmResult confirm(UUID userId, UUID statementId);

    /** Re-runs extraction/parsing after a FAILED statement. */
    Statement retry(UUID userId, UUID statementId);
}
