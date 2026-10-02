package com.finance.domain.statement;

import java.time.Instant;
import java.util.UUID;

/** A user's remembered choice of merchant and/or category for a payee, keyed by PayeeKey. */
public class PayeeRule {

    private final UUID id;
    private final UUID userId;
    private final String matchKey;
    private UUID merchantId;
    private UUID categoryId;
    private final Instant createdAt;

    public PayeeRule(UUID userId, String matchKey, UUID merchantId, UUID categoryId) {
        this(UUID.randomUUID(), userId, matchKey, merchantId, categoryId, null);
    }

    /** Reconstitution constructor - used by the persistence mapper to rebuild a domain object from a stored row. */
    public PayeeRule(UUID id, UUID userId, String matchKey, UUID merchantId, UUID categoryId, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.matchKey = matchKey;
        this.merchantId = merchantId;
        this.categoryId = categoryId;
        this.createdAt = createdAt;
    }

    public void update(UUID merchantId, UUID categoryId) {
        this.merchantId = merchantId;
        this.categoryId = categoryId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getMatchKey() {
        return matchKey;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
