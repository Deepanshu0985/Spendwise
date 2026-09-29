CREATE TABLE statement_transactions (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Denormalized for RLS performance (database-design.md's "Denormalized user_id" rule) -
    -- resolved through the parent statement, not a join, in every policy below.
    user_id                     UUID NOT NULL REFERENCES users(id),
    statement_id                UUID NOT NULL REFERENCES statements(id),
    transaction_date            DATE NOT NULL,
    amount                      NUMERIC(19, 4) NOT NULL,
    currency                    VARCHAR(3) NOT NULL,
    raw_description             VARCHAR(500) NOT NULL,
    normalized_description      VARCHAR(500),
    suggested_merchant_id       UUID REFERENCES merchants(id),
    suggested_category_id       UUID REFERENCES categories(id),
    suggested_transaction_type  VARCHAR(20) NOT NULL,
    confidence_score            NUMERIC(3, 2) NOT NULL,
    duplicate_status            VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    review_status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    canonical_transaction_id    UUID REFERENCES transactions(id),
    source_row_reference        VARCHAR(100) NOT NULL,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_statement_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_statement_transactions_type CHECK (suggested_transaction_type IN (
        'EXPENSE', 'INCOME', 'REFUND', 'FEE_CHARGED', 'INTEREST_CHARGED', 'INTEREST_EARNED',
        'TRANSFER_OUT', 'TRANSFER_IN', 'CARD_PAYMENT_OUT', 'CARD_PAYMENT_IN', 'CASH_WITHDRAWAL', 'UNKNOWN')),
    CONSTRAINT chk_statement_transactions_duplicate_status CHECK (duplicate_status IN (
        'UNKNOWN', 'NOT_DUPLICATE', 'DUPLICATE')),
    CONSTRAINT chk_statement_transactions_review_status CHECK (review_status IN (
        'PENDING', 'ACCEPTED', 'EDITED', 'REJECTED'))
);

CREATE INDEX idx_statement_transactions_statement ON statement_transactions (statement_id);
CREATE INDEX idx_statement_transactions_user_statement ON statement_transactions (user_id, statement_id);

ALTER TABLE statement_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE statement_transactions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant ON statement_transactions
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);
