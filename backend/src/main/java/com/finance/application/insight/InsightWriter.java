package com.finance.application.insight;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

public interface InsightWriter {

    /** The wording for these metrics, or empty if this writer could not produce wording that passes the checks. */
    Optional<InsightText> write(JsonNode metrics);
}
