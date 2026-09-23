package com.finance.auth;

/**
 * Only the raw token can go in the cookie; only its hash is ever persisted. The
 * raw value exists solely in memory for the duration of the create() call that
 * produced it, so it must be returned here rather than reconstructed later.
 */
public record CreatedSession(Session session, String rawToken) {
}
