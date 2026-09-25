package com.finance.domain.user;

import java.time.Instant;
import java.util.UUID;

/** Pure business model - no persistence framework dependency; see infrastructure.persistence.user for the JPA mapping. */
public class User {

    private final UUID id;
    private final String email;
    private String passwordHash;
    private final String fullName;
    private final String defaultCurrency;
    private final String timezone;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;

    public User(String email, String passwordHash, String fullName, String defaultCurrency, String timezone) {
        this(UUID.randomUUID(), email, passwordHash, fullName, defaultCurrency, timezone, null, null, null);
    }

    /** Reconstitution constructor - used by the persistence mapper to rebuild a domain object from a stored row. */
    public User(
            UUID id,
            String email,
            String passwordHash,
            String fullName,
            String defaultCurrency,
            String timezone,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.defaultCurrency = defaultCurrency;
        this.timezone = timezone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public String getTimezone() {
        return timezone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
