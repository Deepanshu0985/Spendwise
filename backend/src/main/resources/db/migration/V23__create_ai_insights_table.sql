-- A written explanation of one month, with the exact figures it was written from and which model and prompt wrote it,
-- so any past text stays attributable and can be checked against its numbers. One row per user, type and period.
CREATE TABLE ai_insights (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id),
    insight_type     VARCHAR(30) NOT NULL,
    title            VARCHAR(200) NOT NULL,
    content          TEXT NOT NULL,
    highlights       JSONB NOT NULL DEFAULT '[]'::jsonb,
    period_start     DATE NOT NULL,
    period_end       DATE NOT NULL,
    supporting_data  JSONB NOT NULL,
    metrics_hash     VARCHAR(64) NOT NULL,
    model_name       VARCHAR(100) NOT NULL,
    prompt_version   VARCHAR(50) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_ai_insights_user_type_period UNIQUE (user_id, insight_type, period_start),
    CONSTRAINT chk_ai_insights_period CHECK (period_end >= period_start)
);

ALTER TABLE ai_insights ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_insights FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant ON ai_insights
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);
