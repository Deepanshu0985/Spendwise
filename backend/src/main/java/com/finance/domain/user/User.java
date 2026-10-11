package com.finance.domain.user;

import java.time.Instant;
import java.util.UUID;

/** Pure business model - no persistence framework dependency; see infrastructure.persistence.user for the JPA mapping. */
public class User {

    private final UUID id;
    private final String email;
    private String passwordHash;
    private String googleSubject;
    private final String fullName;
    private final String defaultCurrency;
    private final String timezone;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;

    public User(String email, String passwordHash, String fullName, String defaultCurrency, String timezone) {
        this(UUID.randomUUID(), email, passwordHash, null, fullName, defaultCurrency, timezone, null, null, null);
    }

    /** An account created by signing in with Google: no password, matched from then on by Google's account id. */
    public static User fromGoogle(String email, String googleSubject, String fullName, String defaultCurrency, String timezone) {
        return new User(UUID.randomUUID(), email, null, googleSubject, fullName, defaultCurrency, timezone, null, null, null);
    }

    /** Reconstitution constructor - used by the persistence mapper to rebuild a domain object from a stored row. */
    public User(
            UUID id,
            String email,
            String passwordHash,
            String googleSubject,
            String fullName,
            String defaultCurrency,
            String timezone,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.googleSubject = googleSubject;
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

    /**
     * Ties this account to a Google account. Any password is dropped: the app never verifies an email address at sign-up,
     * so whoever registered this address first may not be its owner, and keeping their password would let them keep
     * getting in after the real owner proves the address through Google. The owner can set a new one through "Forgot
     * password".
     */
    public void linkGoogle(String googleSubject) {
        this.googleSubject = googleSubject;
        this.passwordHash = null;
    }

    public boolean hasPassword() {
        return passwordHash != null;
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

    public String getGoogleSubject() {
        return googleSubject;
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
