-- "Remember my edits": when a user assigns a merchant/category to a staged row, the choice is stored
-- against the row's normalised description so the next statement applies it automatically.
CREATE TABLE payee_rules (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id),
    match_key    VARCHAR(255) NOT NULL,
    merchant_id  UUID REFERENCES merchants(id),
    category_id  UUID REFERENCES categories(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_payee_rules_user_match_key UNIQUE (user_id, match_key),
    CONSTRAINT chk_payee_rules_has_a_choice CHECK (merchant_id IS NOT NULL OR category_id IS NOT NULL)
);

CREATE INDEX idx_payee_rules_user_id ON payee_rules (user_id);

ALTER TABLE payee_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE payee_rules FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant ON payee_rules
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);
