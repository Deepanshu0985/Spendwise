CREATE TABLE statements (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id),
    account_id     UUID NOT NULL REFERENCES accounts(id),
    file_name      VARCHAR(255) NOT NULL,
    storage_key    VARCHAR(500) NOT NULL,
    file_hash      VARCHAR(64) NOT NULL,
    file_type      VARCHAR(50) NOT NULL,
    period_start   DATE,
    period_end     DATE,
    status         VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    error_message  VARCHAR(1000),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at   TIMESTAMPTZ,
    CONSTRAINT chk_statements_status CHECK (status IN (
        'UPLOADED', 'PROCESSING', 'READY_FOR_REVIEW', 'IMPORTED', 'FAILED')),
    -- file_hash provides upload-level idempotency: re-uploading the identical file is a no-op.
    CONSTRAINT uq_statements_user_file_hash UNIQUE (user_id, file_hash)
);

CREATE INDEX idx_statements_user_created ON statements (user_id, created_at DESC);
CREATE INDEX idx_statements_account ON statements (account_id);

ALTER TABLE statements ENABLE ROW LEVEL SECURITY;
ALTER TABLE statements FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant ON statements
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);
