package com.finance.domain.merchant;

import java.time.Instant;
import java.util.UUID;

/** Pure business model - no persistence framework dependency; see infrastructure.persistence.merchant for the JPA mapping. */
public class Merchant {

    private final UUID id;
    private final UUID userId;
    private String canonicalName;
    private String normalizedKey;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Merchant(UUID userId, String canonicalName, String normalizedKey) {
        this(UUID.randomUUID(), userId, canonicalName, normalizedKey, null, null);
    }

    /** Reconstitution constructor - used by the persistence mapper to rebuild a domain object from a stored row. */
    public Merchant(UUID id, UUID userId, String canonicalName, String normalizedKey, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.canonicalName = canonicalName;
        this.normalizedKey = normalizedKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void rename(String canonicalName, String normalizedKey) {
        this.canonicalName = canonicalName;
        this.normalizedKey = normalizedKey;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getNormalizedKey() {
        return normalizedKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
