-- No RLS on this table - see ADR-019. Session resolution by token hash happens
-- before the app knows which user is making the request; tenant isolation for
-- sessions is enforced by explicit user_id filtering in every query instead.
CREATE TABLE sessions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id),
    token_hash    VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ NOT NULL,
    user_agent    VARCHAR(512),
    revoked_at    TIMESTAMPTZ,
    CONSTRAINT uk_sessions_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_sessions_token_hash ON sessions (token_hash);
CREATE INDEX idx_sessions_user_id ON sessions (user_id);
