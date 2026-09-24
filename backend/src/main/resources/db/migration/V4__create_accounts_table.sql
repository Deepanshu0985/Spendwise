CREATE TABLE accounts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id),
    name              VARCHAR(255) NOT NULL,
    account_type      VARCHAR(20) NOT NULL,
    institution_name  VARCHAR(255),
    last4             VARCHAR(4),
    currency          VARCHAR(3) NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_accounts_account_type CHECK (account_type IN ('BANK', 'CREDIT_CARD', 'CASH', 'WALLET', 'OTHER'))
);

CREATE INDEX idx_accounts_user_id ON accounts (user_id);

-- First genuinely RLS-protected table (ADR-019 excluded Phase 1's three tables).
-- Only effective because the runtime role is no longer BYPASSRLS - see DECISIONS.md.
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE accounts FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant ON accounts
    USING (user_id = current_setting('app.current_user_id', true)::uuid)
    WITH CHECK (user_id = current_setting('app.current_user_id', true)::uuid);
