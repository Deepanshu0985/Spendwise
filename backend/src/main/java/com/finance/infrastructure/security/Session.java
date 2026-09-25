package com.finance.infrastructure.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** See ADR-019: no RLS on this table - tenant isolation is enforced by explicit user_id filtering. */
@Entity
@Table(name = "sessions")
public class Session {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected Session() {
        // JPA
    }

    public Session(UUID userId, String tokenHash, Instant expiresAt, String userAgent) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.lastSeenAt = Instant.now();
        this.expiresAt = expiresAt;
        this.userAgent = userAgent;
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void touch(Instant now) {
        this.lastSeenAt = now;
    }

    public void revoke(Instant now) {
        this.revokedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
