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

    /**
     * Remembers the choice on every given row, one rule per payee. A value the row does not have never wipes what an
     * existing rule already holds. Returns what was created and what each updated rule held before, so it can be undone.
     */
    RuleChange rememberAll(UUID userId, List<StatementTransaction> rows);

    /** Reverses a {@link #rememberAll}: deletes the rules it created and restores the ones it updated. */
    void undo(UUID userId, RuleChange change);

    /** A rule's values, as they were. */
    record RuleSnapshot(UUID id, UUID merchantId, UUID categoryId) {
    }

    /** What a rememberAll did, so undo can reverse exactly that. */
    record RuleChange(List<UUID> createdRuleIds, List<RuleSnapshot> previousRules) {

        public static RuleChange none() {
            return new RuleChange(List.of(), List.of());
        }
    }
}
