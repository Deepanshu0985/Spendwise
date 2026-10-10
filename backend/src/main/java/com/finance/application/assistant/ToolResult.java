package com.finance.application.assistant;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * What a tool produced (typed, deterministic), a one-line value-free note of what it looked at, and - for search - the
 * transactions it returned, kept out of band so the user can be shown them without the model ever handling record ids.
 */
public record ToolResult(JsonNode data, String context, List<SourceRow> sources) {

    public ToolResult(JsonNode data, String context) {
        this(data, context, List.of());
    }

    /** A transaction an answer looked at, as shown to the user. */
    public record SourceRow(java.util.UUID id, LocalDate date, String description, String merchant, BigDecimal amount, String currency, String type) {
    }
}
