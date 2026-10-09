-- A wallet statement (Paytm) lists payments funded from several linked accounts, each row labelled with the account it was paid
-- from. The label is kept on the staged row, and the user maps each label to one of their accounts once; the choice is remembered.
ALTER TABLE statement_transactions
    ADD COLUMN source_account_label VARCHAR(255),
    ADD COLUMN account_id           UUID REFERENCES accounts(id);

CREATE TABLE source_account_rules (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    label_key   VARCHAR(255) NOT NULL,
    account_id  UUID NOT NULL REFERENCES accounts(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_source_account_rules_user_label UNIQUE (user_id, label_key)
);

CREATE INDEX idx_source_account_rules_user_id ON source_account_rules (user_id);

ALTER TABLE source_account_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE source_account_rules FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant ON source_account_rules
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);
