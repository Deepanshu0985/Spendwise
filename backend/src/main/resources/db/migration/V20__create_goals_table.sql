-- A savings goal. current_amount is what the user says they have put aside so far (entered by hand).
CREATE TABLE goals (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id),
    name           VARCHAR(255) NOT NULL,
    target_amount  NUMERIC(19, 2) NOT NULL,
    current_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    target_date    DATE,
    currency       VARCHAR(3) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_goals_target_positive CHECK (target_amount > 0),
    CONSTRAINT chk_goals_current_not_negative CHECK (current_amount >= 0),
    CONSTRAINT chk_goals_status CHECK (status IN ('ACTIVE', 'ACHIEVED'))
);

CREATE INDEX idx_goals_user_id ON goals (user_id);

ALTER TABLE goals ENABLE ROW LEVEL SECURITY;
ALTER TABLE goals FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant ON goals
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);
