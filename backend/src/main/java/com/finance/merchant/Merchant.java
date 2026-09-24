package com.finance.merchant;

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
public class Merchant {

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

    protected Merchant() {
        // JPA
    }

    public Merchant(UUID userId, String canonicalName, String normalizedKey) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.canonicalName = canonicalName;
        this.normalizedKey = normalizedKey;
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
