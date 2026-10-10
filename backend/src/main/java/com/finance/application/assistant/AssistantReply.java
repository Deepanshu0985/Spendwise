package com.finance.application.assistant;

import java.util.List;

/**
 * answer: the text to show. toolsUsed: what the answer was based on (names and the period looked at, never amounts).
 * grounded: every figure in the answer was returned by a tool. fallback: the model's wording could not be verified, so the
 * figures were listed plainly instead. sources: the transactions the search tools returned in this exchange (shown to the user, at most 20). remainingMessagesToday: what is left of the user's daily allowance.
 */
public record AssistantReply(
        String answer, List<ToolUse> toolsUsed, List<ToolResult.SourceRow> sources, boolean grounded, boolean fallback, int remainingMessagesToday) {

    public record ToolUse(String name, String context) {
    }
}
