package com.finance.application.statement;

import com.finance.domain.statement.Statement;
import com.finance.domain.statement.StatementTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface StatementService {

    Statement upload(UUID userId, UploadStatementCommand command);

    /** Names of the banks whose statements can currently be imported, for showing in the upload dialog. */
    List<String> supportedBanks();

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

    /** Leaves one staged row out of the import (it stays visible, marked skipped). Only while the statement awaits review. */
    StatementTransaction skipRow(UUID userId, UUID statementId, UUID stagingId);

    /** Undoes skipRow: the row is imported on confirm again. */
    StatementTransaction restoreRow(UUID userId, UUID statementId, UUID stagingId);

    /**
     * Says which of the user's accounts the rows printed with this source label were paid from (a Paytm row's
     * "Bank Of Baroda - 21"). Applies to every row of the statement with that label, is remembered for future
     * statements, and re-checks duplicates since they depend on the account. Returns all the statement's rows.
     */
    List<StatementTransaction> mapSourceAccount(UUID userId, UUID statementId, String sourceAccountLabel, UUID accountId);

    /** Idempotent: a repeat call against an already-IMPORTED statement returns the original result, never imports twice. */
    ConfirmResult confirm(UUID userId, UUID statementId);

    /** Re-runs extraction/parsing after a FAILED statement; password (nullable) opens an encrypted PDF in memory only and is never stored. */
    Statement retry(UUID userId, UUID statementId, String password);
}
