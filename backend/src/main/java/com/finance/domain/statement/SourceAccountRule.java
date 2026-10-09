package com.finance.domain.statement;

import java.util.UUID;

/** A user's remembered answer to "which of my accounts is this statement's 'Bank Of Baroda - 21' label?", keyed by PayeeKey. */
public class SourceAccountRule {

    private final UUID id;
    private final UUID userId;
    private final String labelKey;
    private UUID accountId;

    public SourceAccountRule(UUID userId, String labelKey, UUID accountId) {
        this(UUID.randomUUID(), userId, labelKey, accountId);
    }

    /** Reconstitution constructor - used by the persistence mapper to rebuild a domain object from a stored row. */
    public SourceAccountRule(UUID id, UUID userId, String labelKey, UUID accountId) {
        this.id = id;
        this.userId = userId;
        this.labelKey = labelKey;
        this.accountId = accountId;
    }

    public void pointTo(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public UUID getAccountId() {
        return accountId;
    }
}
