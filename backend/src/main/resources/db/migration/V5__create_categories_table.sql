CREATE TABLE categories (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID REFERENCES users(id),
    parent_id      UUID REFERENCES categories(id),
    name           VARCHAR(255) NOT NULL,
    category_type  VARCHAR(10) NOT NULL,
    is_system      BOOLEAN NOT NULL DEFAULT false,
    is_active      BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_categories_category_type CHECK (category_type IN ('EXPENSE', 'INCOME')),
    CONSTRAINT chk_categories_system_no_owner CHECK (NOT is_system OR user_id IS NULL)
);

CREATE INDEX idx_categories_user_id ON categories (user_id);
CREATE INDEX idx_categories_parent_id ON categories (parent_id);

ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE categories FORCE ROW LEVEL SECURITY;

-- Split by command, not one blanket USING clause - database-design.md's original
-- single-policy sketch (`USING (is_system OR user_id = ...)`) reads fine for
-- SELECT but is a real gap for UPDATE/DELETE: without a narrower WITH CHECK,
-- any authenticated user could rewrite or delete a shared system row, since
-- is_system alone would satisfy that same USING clause on those commands too.
CREATE POLICY categories_select ON categories
    FOR SELECT
    USING (is_system OR user_id = current_setting('app.current_user_id', true)::uuid);

CREATE POLICY categories_insert ON categories
    FOR INSERT
    WITH CHECK (NOT is_system AND user_id = current_setting('app.current_user_id', true)::uuid);

CREATE POLICY categories_update ON categories
    FOR UPDATE
    USING (NOT is_system AND user_id = current_setting('app.current_user_id', true)::uuid)
    WITH CHECK (NOT is_system AND user_id = current_setting('app.current_user_id', true)::uuid);

CREATE POLICY categories_delete ON categories
    FOR DELETE
    USING (NOT is_system AND user_id = current_setting('app.current_user_id', true)::uuid);

-- Default system categories (ADR-015). 18 rows, not 19: "Uncategorized" is not
-- a row here - see database-design.md. Runs under the migration role
-- (BYPASSRLS), so these INSERTs are unaffected by the policies above.
INSERT INTO categories (name, category_type, is_system) VALUES
    ('Food & Dining', 'EXPENSE', true),
    ('Groceries', 'EXPENSE', true),
    ('Transportation', 'EXPENSE', true),
    ('Shopping', 'EXPENSE', true),
    ('Bills & Utilities', 'EXPENSE', true),
    ('Rent/Housing', 'EXPENSE', true),
    ('Entertainment', 'EXPENSE', true),
    ('Health & Fitness', 'EXPENSE', true),
    ('Travel', 'EXPENSE', true),
    ('Education', 'EXPENSE', true),
    ('Personal Care', 'EXPENSE', true),
    ('Subscriptions', 'EXPENSE', true),
    ('Insurance', 'EXPENSE', true),
    ('Fees & Charges', 'EXPENSE', true),
    ('Salary', 'INCOME', true),
    ('Interest', 'INCOME', true),
    ('Refunds', 'INCOME', true),
    ('Other Income', 'INCOME', true);
