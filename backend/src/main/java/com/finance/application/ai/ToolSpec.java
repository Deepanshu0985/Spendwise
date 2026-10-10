package com.finance.application.ai;

import java.util.Map;

/** A tool as offered to the model: a name, what it does, and a JSON-schema description of its arguments. */
public record ToolSpec(String name, String description, Map<String, Object> parameters) {
}
