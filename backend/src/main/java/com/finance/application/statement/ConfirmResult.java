package com.finance.application.statement;

import com.finance.domain.statement.Statement;
import com.finance.domain.transaction.Transaction;

import java.util.List;

/** skippedDuplicates: rows left out because they match an already-imported transaction and the user didn't keep them. */
public record ConfirmResult(Statement statement, List<Transaction> importedTransactions, int skippedDuplicates) {
}
