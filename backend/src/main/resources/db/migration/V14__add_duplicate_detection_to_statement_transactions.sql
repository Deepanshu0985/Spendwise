-- Phase 7: duplicate detection. The reference (e.g. a UPI RRN) is kept on the staged row so
-- it can be matched against already-imported transactions' external_transaction_id, including
-- ones in a different account - the same UPI payment shows up in both a wallet statement and
-- the bank statement it was paid from.
ALTER TABLE statement_transactions
    ADD COLUMN external_reference           VARCHAR(255),
    ADD COLUMN duplicate_reason             VARCHAR(30),
    ADD COLUMN duplicate_of_transaction_id  UUID REFERENCES transactions(id),
    ADD COLUMN duplicate_overridden_at      TIMESTAMPTZ;

ALTER TABLE statement_transactions DROP CONSTRAINT chk_statement_transactions_duplicate_status;
ALTER TABLE statement_transactions ADD CONSTRAINT chk_statement_transactions_duplicate_status
    CHECK (duplicate_status IN ('UNKNOWN', 'NOT_DUPLICATE', 'POSSIBLE_DUPLICATE', 'DUPLICATE'));
ALTER TABLE statement_transactions ADD CONSTRAINT chk_statement_transactions_duplicate_reason
    CHECK (duplicate_reason IS NULL OR duplicate_reason IN ('EXACT_REFERENCE', 'DATE_AMOUNT_DESCRIPTION', 'NEARBY_SIMILAR'));

CREATE INDEX idx_transactions_user_external_id ON transactions (user_id, external_transaction_id)
    WHERE external_transaction_id IS NOT NULL;
