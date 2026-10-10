package com.finance.application.insight;

import java.util.List;

/** The words of an insight: a title, a short summary and a few highlights. Contains no figures of its own - see InsightMetrics. */
public record InsightText(String title, String summary, List<String> highlights) {
}
