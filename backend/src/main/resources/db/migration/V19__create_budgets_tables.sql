-- A budget: a total limit over a period, optionally with a limit per category. MONTHLY budgets repeat every calendar
-- month (end_date is null); CUSTOM ones cover a fixed start_date..end_date.
CREATE TABLE budgets (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id),
    name          VARCHAR(255) NOT NULL,
    period_type   VARCHAR(20) NOT NULL,
    start_date    DATE NOT NULL,
    end_date      DATE,
    total_limit   NUMERIC(19, 2) NOT NULL,
    currency      VARCHAR(3) NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_budgets_period_type CHECK (period_type IN ('MONTHLY', 'CUSTOM')),
    CONSTRAINT chk_budgets_total_limit_positive CHECK (total_limit > 0),
    CONSTRAINT chk_budgets_custom_has_end CHECK (period_type = 'MONTHLY' OR end_date >= start_date)
);

CREATE INDEX idx_budgets_user_id ON budgets (user_id);

CREATE TABLE budget_categories (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id),
    budget_id     UUID NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    category_id   UUID NOT NULL REFERENCES categories(id),
    limit_amount  NUMERIC(19, 2) NOT NULL,
    CONSTRAINT uk_budget_categories_budget_category UNIQUE (budget_id, category_id),
    CONSTRAINT chk_budget_categories_limit_positive CHECK (limit_amount > 0)
);

CREATE INDEX idx_budget_categories_budget_id ON budget_categories (budget_id);

ALTER TABLE budgets ENABLE ROW LEVEL SECURITY;
ALTER TABLE budgets FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant ON budgets
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

ALTER TABLE budget_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE budget_categories FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant ON budget_categories
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);
