-- Staged rows can carry a model-suggested category (flagged so the screen can say so; cleared when the user edits the row).
ALTER TABLE statement_transactions
    ADD COLUMN ai_suggested BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN ai_reason    VARCHAR(500);

-- Rows sent to the model, counted per user per day (the per-user daily cap) ...
CREATE TABLE ai_usage_daily (
    user_id        UUID NOT NULL REFERENCES users(id),
    usage_date     DATE NOT NULL,
    rows_classified INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, usage_date),
    CONSTRAINT chk_ai_usage_daily_not_negative CHECK (rows_classified >= 0)
);

ALTER TABLE ai_usage_daily ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_usage_daily FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant ON ai_usage_daily
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

-- ... and across everyone per month (the global cost ceiling). A bare counter: no user data, so no tenant policy.
CREATE TABLE ai_usage_monthly (
    usage_month     DATE PRIMARY KEY,
    rows_classified INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_ai_usage_monthly_not_negative CHECK (rows_classified >= 0)
);
