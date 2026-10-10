package com.finance.infrastructure.web.insight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.application.insight.InsightView;
import com.finance.domain.insight.AiInsight;

import java.time.Instant;
import java.util.List;

/**
 * writtenByAi: false when the wording came from the fixed template. note: why AI wording was not used, when it was not.
 * cached: an earlier insight was reused because the figures are unchanged. metrics: the exact figures the wording was written
 * from (computed by the application) - the screen draws its charts from these, so the charts and the words always agree.
 */
public record InsightResponse(
        String month, String title, String summary, List<String> highlights, JsonNode metrics, boolean writtenByAi, String modelName,
        String promptVersion, Instant generatedAt, boolean cached, String note) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static InsightResponse from(InsightView view) {
        return from(view.insight(), view.cached(), view.note());
    }

    public static InsightResponse from(AiInsight insight, boolean cached, String note) {
        return new InsightResponse(
                insight.getPeriodStart().getYear() + "-" + String.format("%02d", insight.getPeriodStart().getMonthValue()), insight.getTitle(),
                insight.getContent(), insight.getHighlights(), parse(insight.getSupportingData()), insight.writtenByModel(), insight.getModelName(),
                insight.getPromptVersion(), insight.getUpdatedAt(), cached, note);
    }

    private static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            return MAPPER.createObjectNode();
        }
    }
}
