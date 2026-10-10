package com.finance.application.ai;

/** What the model said, plus the token counts it reported (kept for the cost-regression checks in ai-evaluation.md). */
public record ModelResult(String text, int inputTokens, int outputTokens) {
}
