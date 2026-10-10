package com.finance.infrastructure.web.insight;

import com.finance.application.insight.InsightView;
import com.finance.domain.insight.AiInsight;

import java.time.Instant;
import java.util.List;

/**
 * writtenByAi: false when the wording came from the fixed template. note: why AI wording was not used, when it was not.
 * cached: an earlier insight was reused because the figures are unchanged.
 */
public record InsightResponse(
        String month, String title, String summary, List<String> highlights, boolean writtenByAi, String modelName, String promptVersion,
        Instant generatedAt, boolean cached, String note) {

    public static InsightResponse from(InsightView view) {
        return from(view.insight(), view.cached(), view.note());
    }

    public static InsightResponse from(AiInsight insight, boolean cached, String note) {
        return new InsightResponse(
                insight.getPeriodStart().getYear() + "-" + String.format("%02d", insight.getPeriodStart().getMonthValue()), insight.getTitle(),
                insight.getContent(), insight.getHighlights(), insight.writtenByModel(), insight.getModelName(), insight.getPromptVersion(),
                insight.getUpdatedAt(), cached, note);
    }
}
