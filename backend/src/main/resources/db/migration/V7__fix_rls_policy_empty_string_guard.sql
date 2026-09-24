-- current_setting('app.current_user_id', true) does not reliably return SQL
-- NULL when the setting is unset - on a connection whose custom GUC namespace
-- was touched by an earlier transaction (routine under connection pooling,
-- including Neon's own pooler), Postgres can return '' instead. Casting
-- ''::uuid throws a hard error rather than making the policy correctly match
-- zero rows. NULLIF(..., '') converts that empty string to a true NULL first,
-- so an unauthenticated/unset context fails closed (no rows) instead of
-- erroring. Confirmed by direct psql reproduction before writing this fix.
-- V4-V6 already ran, so this corrects them in place rather than editing them.

DROP POLICY tenant ON accounts;
CREATE POLICY tenant ON accounts
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

DROP POLICY tenant ON merchants;
CREATE POLICY tenant ON merchants
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

DROP POLICY categories_select ON categories;
CREATE POLICY categories_select ON categories
    FOR SELECT
    USING (is_system OR user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

DROP POLICY categories_insert ON categories;
CREATE POLICY categories_insert ON categories
    FOR INSERT
    WITH CHECK (NOT is_system AND user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

DROP POLICY categories_update ON categories;
CREATE POLICY categories_update ON categories
    FOR UPDATE
    USING (NOT is_system AND user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (NOT is_system AND user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

DROP POLICY categories_delete ON categories;
CREATE POLICY categories_delete ON categories
    FOR DELETE
    USING (NOT is_system AND user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);
