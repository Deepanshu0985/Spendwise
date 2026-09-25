-- Generic mechanism reused across POST /transactions, POST /transactions/
-- transfer, and eventually POST /statements/{id}/confirm (api-specification.md
-- Idempotency section) - stores the full original response so a repeated key
-- returns exactly what the first call returned, rather than re-running
-- business logic a second time.
-- Column named idempotency_key, not key: "key" is a reserved word H2 chokes
-- on unquoted (caught by running the H2-backed test suite before this ever
-- reached Postgres) - avoiding the ambiguity outright rather than relying on
-- Postgres tolerating it.
CREATE TABLE idempotency_keys (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id),
    idempotency_key  VARCHAR(255) NOT NULL,
    endpoint         VARCHAR(255) NOT NULL,
    response_status  INTEGER NOT NULL,
    response_body    TEXT NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_idempotency_keys_user_key_endpoint UNIQUE (user_id, idempotency_key, endpoint)
);

ALTER TABLE idempotency_keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE idempotency_keys FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant ON idempotency_keys
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);
