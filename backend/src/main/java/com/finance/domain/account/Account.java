package com.finance.domain.account;

import java.time.Instant;
import java.util.UUID;

/** Pure business model - no persistence framework dependency; see infrastructure.persistence.account for the JPA mapping. */
public class Account {

    private final UUID id;
    private final UUID userId;
    private String name;
    private final AccountType accountType;
    private String institutionName;
    private String last4;
    private final String currency;
    private boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Account(UUID userId, String name, AccountType accountType, String institutionName, String last4, String currency) {
        this(UUID.randomUUID(), userId, name, accountType, institutionName, last4, currency, true, null, null);
    }

    /** Reconstitution constructor - used by the persistence mapper to rebuild a domain object from a stored row. */
    public Account(
            UUID id,
            UUID userId,
            String name,
            AccountType accountType,
            String institutionName,
            String last4,
            String currency,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.accountType = accountType;
        this.institutionName = institutionName;
        this.last4 = last4;
        this.currency = currency;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(String name, String institutionName, String last4) {
        this.name = name;
        this.institutionName = institutionName;
        this.last4 = last4;
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public String getLast4() {
        return last4;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
