package com.finance.infrastructure.persistence.statement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_account_rules")
public class SourceAccountRuleJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "label_key", nullable = false)
    private String labelKey;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SourceAccountRuleJpaEntity() {
        // JPA
    }

    SourceAccountRuleJpaEntity(UUID id, UUID userId, String labelKey, UUID accountId) {
        this.id = id;
        this.userId = userId;
        this.labelKey = labelKey;
        this.accountId = accountId;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getLabelKey() {
        return labelKey;
    }

    UUID getAccountId() {
        return accountId;
    }

    void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }
}
