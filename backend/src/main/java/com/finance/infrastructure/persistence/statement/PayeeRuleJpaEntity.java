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
@Table(name = "payee_rules")
public class PayeeRuleJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "match_key", nullable = false)
    private String matchKey;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(name = "category_id")
    private UUID categoryId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PayeeRuleJpaEntity() {
        // JPA
    }

    PayeeRuleJpaEntity(UUID id, UUID userId, String matchKey, UUID merchantId, UUID categoryId) {
        this.id = id;
        this.userId = userId;
        this.matchKey = matchKey;
        this.merchantId = merchantId;
        this.categoryId = categoryId;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getMatchKey() {
        return matchKey;
    }

    UUID getMerchantId() {
        return merchantId;
    }

    void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    UUID getCategoryId() {
        return categoryId;
    }

    void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
