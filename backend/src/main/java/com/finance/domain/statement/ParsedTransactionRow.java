package com.finance.domain.statement;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row as a bank parser extracted it, before normalization/classification.
 * debitCredit is consumed during classification and resolved into a
 * TransactionType (transaction-normalization.md's rule 4) - it is never
 * persisted as its own column, matching how transactions.transaction_type
 * already carries the ledger side (ADR-012).
 */
public record ParsedTransactionRow(
        LocalDate transactionDate,
        BigDecimal amount,
        DebitCredit debitCredit,
        String rawDescription,
        String reference,
        BigDecimal balance,
        String sourceRowReference) {

    public enum DebitCredit {
        DEBIT,
        CREDIT
    }
}
