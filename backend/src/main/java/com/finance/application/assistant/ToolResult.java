package com.finance.application.assistant;

import com.fasterxml.jackson.databind.JsonNode;

/** What a tool produced (typed, deterministic) and a one-line, value-free note of what it looked at, shown to the user as context. */
public record ToolResult(JsonNode data, String context) {
}
