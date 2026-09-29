package com.finance.domain.statement;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pure business model - no persistence framework dependency; see
 * infrastructure.persistence.statement for the JPA mapping. Carries the
 * pdf-processing.md state machine as explicit transition methods rather than a
 * bare setter, so an invalid transition is a compile-time-visible mistake to
 * avoid, not just a possible runtime value.
 */
public class Statement {

    private final UUID id;
    private final UUID userId;
    private final UUID accountId;
    private final String fileName;
    private final String storageKey;
    private final String fileHash;
    private final String fileType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private StatementStatus status;
    private String errorMessage;
    private final Instant createdAt;
    private Instant processedAt;

    public Statement(UUID userId, UUID accountId, String fileName, String storageKey, String fileHash, String fileType) {
        this(UUID.randomUUID(), userId, accountId, fileName, storageKey, fileHash, fileType, null, null,
                StatementStatus.UPLOADED, null, null, null);
    }

    /** Reconstitution constructor - used by the persistence mapper to rebuild a domain object from a stored row. */
    public Statement(
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
            Instant createdAt,
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
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public void markProcessing() {
        this.status = StatementStatus.PROCESSING;
    }

    public void markReadyForReview(LocalDate periodStart, LocalDate periodEnd) {
        this.status = StatementStatus.READY_FOR_REVIEW;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.errorMessage = null;
    }

    public void markFailed(String reason) {
        this.status = StatementStatus.FAILED;
        this.errorMessage = reason;
    }

    public void markImported(Instant processedAt) {
        this.status = StatementStatus.IMPORTED;
        this.processedAt = processedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getFileHash() {
        return fileHash;
    }

    public String getFileType() {
        return fileType;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public StatementStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
