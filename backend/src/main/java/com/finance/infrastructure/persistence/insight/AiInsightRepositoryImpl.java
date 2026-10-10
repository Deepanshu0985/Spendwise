package com.finance.infrastructure.persistence.insight;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.domain.insight.AiInsight;
import com.finance.domain.insight.AiInsightRepository;
import com.finance.domain.insight.InsightType;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AiInsightRepositoryImpl implements AiInsightRepository {

    private final AiInsightJpaRepository jpaRepository;
    private final ObjectMapper mapper;

    public AiInsightRepositoryImpl(AiInsightJpaRepository jpaRepository, ObjectMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public AiInsight save(AiInsight insight) {
        AiInsightJpaEntity entity = jpaRepository.findById(insight.getId())
                .orElseGet(() -> new AiInsightJpaEntity(insight.getId(), insight.getUserId(), insight.getType(), insight.getPeriodStart(), insight.getPeriodEnd()));
        entity.setTitle(insight.getTitle());
        entity.setContent(insight.getContent());
        entity.setHighlights(toJson(insight.getHighlights()));
        entity.setSupportingData(insight.getSupportingData());
        entity.setMetricsHash(insight.getMetricsHash());
        entity.setModelName(insight.getModelName());
        entity.setPromptVersion(insight.getPromptVersion());
        return toDomain(jpaRepository.saveAndFlush(entity));
    }

    @Override
    public Optional<AiInsight> findByUserIdAndTypeAndPeriodStart(UUID userId, InsightType type, LocalDate periodStart) {
        return jpaRepository.findByUserIdAndTypeAndPeriodStart(userId, type, periodStart).map(this::toDomain);
    }

    private AiInsight toDomain(AiInsightJpaEntity e) {
        return new AiInsight(
                e.getId(), e.getUserId(), e.getType(), e.getTitle(), e.getContent(), fromJson(e.getHighlights()), e.getPeriodStart(), e.getPeriodEnd(),
                e.getSupportingData(), e.getMetricsHash(), e.getModelName(), e.getPromptVersion(), e.getUpdatedAt());
    }

    private String toJson(List<String> list) {
        try {
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new IllegalStateException("Could not store insight highlights", e);
        }
    }

    private List<String> fromJson(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception e) {
            return List.of();
        }
    }
}
