package com.finance.domain.insight;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A stored month explanation. supportingData is the exact figures (as JSON text) the wording was produced from and metricsHash
 * identifies them, so a cached insight is reused only while the figures are unchanged. modelName is "deterministic" and
 * promptVersion a template version when no model wrote it.
 */
public class AiInsight {

    public static final String DETERMINISTIC_MODEL = "deterministic";

    private final UUID id;
    private final UUID userId;
    private final InsightType type;
    private String title;
    private String content;
    private List<String> highlights;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private String supportingData;
    private String metricsHash;
    private String modelName;
    private String promptVersion;
    private Instant updatedAt;

    public AiInsight(
            UUID userId, InsightType type, String title, String content, List<String> highlights, LocalDate periodStart, LocalDate periodEnd,
            String supportingData, String metricsHash, String modelName, String promptVersion) {
        this(UUID.randomUUID(), userId, type, title, content, highlights, periodStart, periodEnd, supportingData, metricsHash, modelName, promptVersion, null);
    }

    /** Reconstitution constructor - used by the persistence mapper to rebuild a domain object from a stored row. */
    public AiInsight(
            UUID id, UUID userId, InsightType type, String title, String content, List<String> highlights, LocalDate periodStart,
            LocalDate periodEnd, String supportingData, String metricsHash, String modelName, String promptVersion, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.highlights = List.copyOf(highlights);
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.supportingData = supportingData;
        this.metricsHash = metricsHash;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.updatedAt = updatedAt;
    }

    public void rewrite(
            String title, String content, List<String> highlights, String supportingData, String metricsHash, String modelName, String promptVersion) {
        this.title = title;
        this.content = content;
        this.highlights = List.copyOf(highlights);
        this.supportingData = supportingData;
        this.metricsHash = metricsHash;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
    }

    public boolean writtenByModel() {
        return !DETERMINISTIC_MODEL.equals(modelName);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public InsightType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public List<String> getHighlights() {
        return highlights;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public String getSupportingData() {
        return supportingData;
    }

    public String getMetricsHash() {
        return metricsHash;
    }

    public String getModelName() {
        return modelName;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
