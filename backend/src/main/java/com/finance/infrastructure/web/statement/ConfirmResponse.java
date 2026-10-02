package com.finance.infrastructure.web.statement;

import com.finance.application.statement.ConfirmResult;
import com.finance.domain.transaction.Transaction;

import java.util.List;
import java.util.UUID;

public record ConfirmResponse(StatementResponse statement, List<UUID> importedTransactionIds, int skippedDuplicateCount) {

    public static ConfirmResponse from(ConfirmResult result) {
        return new ConfirmResponse(
                StatementResponse.from(result.statement()),
                result.importedTransactions().stream().map(Transaction::getId).toList(),
                result.skippedDuplicates());
    }
}
