-- A statement row can now be flagged as a possible duplicate of a transaction the user typed in by hand.
ALTER TABLE statement_transactions DROP CONSTRAINT chk_statement_transactions_duplicate_reason;
ALTER TABLE statement_transactions ADD CONSTRAINT chk_statement_transactions_duplicate_reason
    CHECK (duplicate_reason IS NULL OR duplicate_reason IN (
        'EXACT_REFERENCE', 'DATE_AMOUNT_DESCRIPTION', 'NEARBY_SIMILAR', 'MANUAL_ENTRY_MATCH'));
