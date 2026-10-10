package com.finance.application.insight;

import com.finance.domain.insight.AiInsight;

/** cached: an earlier insight was reused because nothing it was written from has changed. note: why AI wording was not used, if it was not. */
public record InsightView(AiInsight insight, boolean cached, String note) {
}
