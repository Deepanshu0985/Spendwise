-- Payments detected as repeating (subscriptions, EMIs, bills). Rebuilt from confirmed transactions on demand;
-- group_key identifies the payee so a user's confirm / dismiss decision survives every re-detection.
CREATE TABLE recurring_expenses (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    group_key           VARCHAR(300) NOT NULL,
    merchant_id         UUID REFERENCES merchants(id),
    category_id         UUID REFERENCES categories(id),
    name                VARCHAR(255) NOT NULL,
    currency            VARCHAR(3) NOT NULL,
    average_amount      NUMERIC(19, 2) NOT NULL,
    frequency           VARCHAR(20) NOT NULL,
    last_seen_date      DATE NOT NULL,
    next_expected_date  DATE NOT NULL,
    monthly_estimate    NUMERIC(19, 2) NOT NULL,
    yearly_estimate     NUMERIC(19, 2) NOT NULL,
    occurrences         INTEGER NOT NULL,
    confidence_score    NUMERIC(3, 2) NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    confirmed           BOOLEAN NOT NULL DEFAULT FALSE,
    dismissed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_recurring_expenses_user_group UNIQUE (user_id, group_key),
    CONSTRAINT chk_recurring_expenses_frequency CHECK (frequency IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY'))
);

CREATE INDEX idx_recurring_expenses_user_next ON recurring_expenses (user_id, next_expected_date);

ALTER TABLE recurring_expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE recurring_expenses FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant ON recurring_expenses
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);
