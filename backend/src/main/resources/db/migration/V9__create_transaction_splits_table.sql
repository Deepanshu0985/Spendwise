-- Ships from V1 of the schema per ADR-013, even though split-editing UI is
-- deferred: adding this later is cheap only if every category aggregation is
-- centralized from the start (the transaction_category_allocations view,
-- next migration). The invariant that splits sum to the parent transaction's
-- amount is enforced in the service layer (TransactionServiceImpl), not a
-- database trigger - see ADR-013.
CREATE TABLE transaction_splits (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    transaction_id  UUID NOT NULL REFERENCES transactions(id),
    category_id     UUID REFERENCES categories(id),
    amount          NUMERIC(19, 4) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_transaction_splits_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_transaction_splits_transaction_id ON transaction_splits (transaction_id);
CREATE INDEX idx_transaction_splits_user_id ON transaction_splits (user_id);

ALTER TABLE transaction_splits ENABLE ROW LEVEL SECURITY;
ALTER TABLE transaction_splits FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant ON transaction_splits
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);
