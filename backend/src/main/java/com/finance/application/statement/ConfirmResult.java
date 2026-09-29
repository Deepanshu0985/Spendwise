package com.finance.application.statement;

import com.finance.domain.statement.Statement;
import com.finance.domain.transaction.Transaction;

import java.util.List;

public record ConfirmResult(Statement statement, List<Transaction> importedTransactions) {
}
