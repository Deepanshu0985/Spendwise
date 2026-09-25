package com.finance.infrastructure.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** Named *Record, not IdempotencyKey, to avoid colliding with the plain string key value it stores (see the `key` field). */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyRecord {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "idempotency_key", nullable = false)
    private String key;

    @Column(nullable = false)
    private String endpoint;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    // No @Lob: for a plain String, that maps to Postgres's oid large-object
    // type by default, not the TEXT column the migration actually creates -
    // caught by Hibernate's own schema validation failing at boot (schema
    // mismatch), not by reading the annotation. columnDefinition pins it to
    // exactly what the migration has.
    @Column(name = "response_body", nullable = false, columnDefinition = "text")
    private String responseBody;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyKeyRecord() {
        // JPA
    }

    public IdempotencyKeyRecord(UUID userId, String key, String endpoint, int responseStatus, String responseBody) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.key = key;
        this.endpoint = endpoint;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getKey() {
        return key;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
