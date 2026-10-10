-- The usage caps now cover two kinds of AI use with separate allowances: rows sent for categorisation, and chat messages
-- to the assistant. Existing counts are categorisation counts.
ALTER TABLE ai_usage_daily ADD COLUMN usage_kind VARCHAR(20) NOT NULL DEFAULT 'CATEGORIZATION';
ALTER TABLE ai_usage_daily DROP CONSTRAINT ai_usage_daily_pkey;
ALTER TABLE ai_usage_daily ADD PRIMARY KEY (user_id, usage_date, usage_kind);

ALTER TABLE ai_usage_monthly ADD COLUMN usage_kind VARCHAR(20) NOT NULL DEFAULT 'CATEGORIZATION';
ALTER TABLE ai_usage_monthly DROP CONSTRAINT ai_usage_monthly_pkey;
ALTER TABLE ai_usage_monthly ADD PRIMARY KEY (usage_month, usage_kind);
