package com.finance.application.insight;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Everything an insight may say, computed by the application: the figures and the unusual-spending findings. hash identifies
 * these figures and the wording versions, so a stored insight is reused only while nothing it was written from has changed.
 */
public record InsightMetrics(ObjectNode json, String hash, boolean hasActivity) {
}
