package com.finance.auth;

import java.util.Optional;
import java.util.UUID;

/**
 * Per lld.md, adapted: create() must also hand back the raw token (see
 * CreatedSession) since only its hash is persisted and the caller needs the raw
 * value to set as the cookie. resolve() only returns a session that is
 * currently active (not expired or revoked).
 */
public interface SessionStore {

    CreatedSession create(UUID userId, String userAgent);

    Optional<Session> resolve(String rawToken);

    void revoke(UUID sessionId);

    /** Refreshes last_seen_at, the basis for the idle timeout. */
    void touch(UUID sessionId);
}
