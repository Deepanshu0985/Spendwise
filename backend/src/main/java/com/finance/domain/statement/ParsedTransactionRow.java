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
        String sourceRowReference,
        String accountLabel) {

    /** Row from a statement that does not say which account each payment came from (accountLabel stays null). */
    public ParsedTransactionRow(
            LocalDate transactionDate, BigDecimal amount, DebitCredit debitCredit, String rawDescription,
            String reference, BigDecimal balance, String sourceRowReference) {
        this(transactionDate, amount, debitCredit, rawDescription, reference, balance, sourceRowReference, null);
    }

    public enum DebitCredit {
        DEBIT,
        CREDIT
    }
}
