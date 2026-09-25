CREATE TABLE transactions (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID NOT NULL REFERENCES users(id),
    account_id               UUID NOT NULL REFERENCES accounts(id),
    merchant_id              UUID REFERENCES merchants(id),
    category_id              UUID REFERENCES categories(id),
    transaction_date         DATE NOT NULL,
    amount                   NUMERIC(19, 4) NOT NULL,
    currency                 VARCHAR(3) NOT NULL,
    description              VARCHAR(500),
    raw_description          VARCHAR(500),
    transaction_type         VARCHAR(20) NOT NULL,
    payment_method           VARCHAR(50),
    source                   VARCHAR(20) NOT NULL,
    source_reference         VARCHAR(255),
    external_transaction_id  VARCHAR(255),
    transfer_group_id        UUID,
    confidence_score         NUMERIC(3, 2),
    status                   VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Positive magnitude only; the type supplies the ledger side (ADR-012,
    -- database-design.md's Financial Rules).
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transactions_type CHECK (transaction_type IN (
        'EXPENSE', 'INCOME', 'REFUND', 'FEE_CHARGED', 'INTEREST_CHARGED', 'INTEREST_EARNED',
        'TRANSFER_OUT', 'TRANSFER_IN', 'CARD_PAYMENT_OUT', 'CARD_PAYMENT_IN', 'CASH_WITHDRAWAL', 'UNKNOWN')),
    CONSTRAINT chk_transactions_source CHECK (source IN ('MANUAL', 'STATEMENT', 'IMPORT', 'SYSTEM')),
    CONSTRAINT chk_transactions_status CHECK (status IN ('PENDING', 'CONFIRMED', 'IGNORED', 'DELETED'))
);

CREATE INDEX idx_transactions_user_date ON transactions (user_id, transaction_date DESC);
CREATE INDEX idx_transactions_account_date ON transactions (account_id, transaction_date DESC);
CREATE INDEX idx_transactions_category_date ON transactions (category_id, transaction_date DESC);
CREATE INDEX idx_transactions_merchant_date ON transactions (merchant_id, transaction_date DESC);
CREATE INDEX idx_transactions_user_type ON transactions (user_id, transaction_type);
CREATE INDEX idx_transactions_transfer_group ON transactions (transfer_group_id);

ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant ON transactions
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);
