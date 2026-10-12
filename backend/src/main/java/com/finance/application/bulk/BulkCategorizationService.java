package com.finance.application.bulk;

import com.finance.application.statement.PayeeRuleService.RuleChange;
import com.finance.domain.statement.ReviewStatus;
import com.finance.domain.statement.StatementTransaction;
import com.finance.domain.transaction.Transaction;

import java.util.List;
import java.util.UUID;

/**
 * One operation for "set the same category (and/or merchant) on many rows": on a statement awaiting review, and on saved
 * transactions. Every row is checked to belong to the person (and the statement to still be awaiting review) before anything
 * is changed, the whole batch is one database transaction, and each change returns what it replaced so it can be undone.
 */
public interface BulkCategorizationService {

    int MAX_ROWS = 500;

    StagedResult changeStagedRows(UUID userId, UUID statementId, StagedChange change);

    /** Puts back what a changeStagedRows replaced. */
    List<StatementTransaction> undoStagedRows(UUID userId, UUID statementId, StagedUndo undo);

    LedgerResult changeTransactions(UUID userId, LedgerChange change);

    List<Transaction> undoTransactions(UUID userId, List<TransactionSnapshot> snapshots);

    /** categoryId and/or merchantId (at least one); rememberPayee also stores the choice per payee and fills the other untouched rows of that payee. */
    record StagedChange(List<UUID> rowIds, UUID categoryId, UUID merchantId, boolean rememberPayee) {
    }

    record RowSnapshot(UUID id, UUID merchantId, UUID categoryId, ReviewStatus reviewStatus, boolean aiSuggested, String aiReason) {
    }

    record StagedUndo(List<RowSnapshot> rows, RuleChange rules) {
    }

    /** rows is every row of the statement after the change, so the screen can redraw from it. */
    record StagedResult(List<StatementTransaction> rows, int changedCount, StagedUndo undo) {
    }

    record LedgerChange(List<UUID> transactionIds, UUID categoryId, UUID merchantId) {
    }

    record TransactionSnapshot(UUID id, UUID merchantId, UUID categoryId) {
    }

    /** skippedWithSplits: transactions whose amount is divided across categories are left alone (their category lives on the splits). */
    record LedgerResult(List<Transaction> changed, int skippedWithSplits, List<TransactionSnapshot> undo) {
    }
}
