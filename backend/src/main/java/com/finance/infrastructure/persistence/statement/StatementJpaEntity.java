package com.finance.infrastructure.persistence.statement;

import com.finance.domain.statement.StatementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "statements")
public class StatementJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "file_hash", nullable = false)
    private String fileHash;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatementStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected StatementJpaEntity() {
        // JPA
    }

    StatementJpaEntity(
            UUID id,
            UUID userId,
            UUID accountId,
            String fileName,
            String storageKey,
            String fileHash,
            String fileType,
            LocalDate periodStart,
            LocalDate periodEnd,
            StatementStatus status,
            String errorMessage,
            Instant processedAt) {
        this.id = id;
        this.userId = userId;
        this.accountId = accountId;
        this.fileName = fileName;
        this.storageKey = storageKey;
        this.fileHash = fileHash;
        this.fileType = fileType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.status = status;
        this.errorMessage = errorMessage;
        this.processedAt = processedAt;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    UUID getAccountId() {
        return accountId;
    }

    String getFileName() {
        return fileName;
    }

    String getStorageKey() {
        return storageKey;
    }

    String getFileHash() {
        return fileHash;
    }

    String getFileType() {
        return fileType;
    }

    LocalDate getPeriodStart() {
        return periodStart;
    }

    void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    LocalDate getPeriodEnd() {
        return periodEnd;
    }

    void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    StatementStatus getStatus() {
        return status;
    }

    void setStatus(StatementStatus status) {
        this.status = status;
    }

    String getErrorMessage() {
        return errorMessage;
    }

    void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getProcessedAt() {
        return processedAt;
    }

    void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
