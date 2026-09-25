package com.finance.infrastructure.security;

/**
 * Deterministic hashing for session and password-reset tokens - not
 * password hashing. Tokens are already high-entropy random secrets, so a fast
 * deterministic hash (allowing an indexed lookup by hash) is the right and
 * standard choice here; the slow, salted, adaptive hashing passwords need
 * (BCryptPasswordEncoder) would make looking up a session by its hash
 * impractical, and isn't needed for a secret that isn't guessable to begin with.
 */
public interface TokenHasher {

    String hash(String rawToken);
}
