package com.finance.application.ai;

/** A model's request to run one tool. argumentsJson is exactly what the model produced and is untrusted until validated. */
public record ToolCall(String id, String name, String argumentsJson) {
}
