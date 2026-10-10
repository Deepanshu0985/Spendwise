package com.finance.application.ai;

import java.util.List;

/** The model's reply: either some text, or a request to run tools (or, rarely, both). */
public record ChatResult(String content, List<ToolCall> toolCalls, int inputTokens, int outputTokens) {
}
