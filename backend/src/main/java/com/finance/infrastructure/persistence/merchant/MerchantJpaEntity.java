package com.finance.infrastructure.persistence.merchant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchants")
public class MerchantJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "canonical_name", nullable = false)
    private String canonicalName;

    @Column(name = "normalized_key", nullable = false)
    private String normalizedKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MerchantJpaEntity() {
        // JPA
    }

    MerchantJpaEntity(UUID id, UUID userId, String canonicalName, String normalizedKey) {
        this.id = id;
        this.userId = userId;
        this.canonicalName = canonicalName;
        this.normalizedKey = normalizedKey;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getCanonicalName() {
        return canonicalName;
    }

    void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    String getNormalizedKey() {
        return normalizedKey;
    }

    void setNormalizedKey(String normalizedKey) {
        this.normalizedKey = normalizedKey;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
