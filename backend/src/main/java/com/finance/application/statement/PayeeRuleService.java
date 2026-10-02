package com.finance.application.statement;

import com.finance.domain.statement.StatementTransaction;

import java.util.List;
import java.util.UUID;

public interface PayeeRuleService {

    /** Stores the merchant/category the user chose on a reviewed row against its payee, replacing any earlier choice. */
    void remember(UUID userId, StatementTransaction reviewedRow);

    /**
     * Fills the merchant/category on rows the user hasn't touched, from remembered rules.
     * Mutates the rows and returns the ones that changed (the caller persists them).
     */
    List<StatementTransaction> applyTo(UUID userId, List<StatementTransaction> rows);
}
