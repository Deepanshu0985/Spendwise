package com.finance.infrastructure.persistence.insight;

import com.finance.domain.insight.InsightType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ai_insights")
public class AiInsightJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "insight_type", nullable = false)
    private InsightType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String highlights;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supporting_data", nullable = false, columnDefinition = "jsonb")
    private String supportingData;

    @Column(name = "metrics_hash", nullable = false)
    private String metricsHash;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "prompt_version", nullable = false)
    private String promptVersion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AiInsightJpaEntity() {
        // JPA
    }

    AiInsightJpaEntity(UUID id, UUID userId, InsightType type, LocalDate periodStart, LocalDate periodEnd) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    InsightType getType() { return type; }
    String getTitle() { return title; }
    String getContent() { return content; }
    String getHighlights() { return highlights; }
    LocalDate getPeriodStart() { return periodStart; }
    LocalDate getPeriodEnd() { return periodEnd; }
    String getSupportingData() { return supportingData; }
    String getMetricsHash() { return metricsHash; }
    String getModelName() { return modelName; }
    String getPromptVersion() { return promptVersion; }
    Instant getUpdatedAt() { return updatedAt; }

    void setTitle(String title) { this.title = title; }
    void setContent(String content) { this.content = content; }
    void setHighlights(String highlights) { this.highlights = highlights; }
    void setSupportingData(String supportingData) { this.supportingData = supportingData; }
    void setMetricsHash(String metricsHash) { this.metricsHash = metricsHash; }
    void setModelName(String modelName) { this.modelName = modelName; }
    void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
}
