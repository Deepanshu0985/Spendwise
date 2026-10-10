package com.finance.application.ai;

import java.util.List;

/**
 * One turn of a model conversation. role is user, assistant or tool. An assistant turn may carry toolCalls; a tool turn
 * carries the result for one earlier call (toolCallId, toolName). The system prompt is passed separately and never here.
 */
public record ChatMessage(String role, String content, List<ToolCall> toolCalls, String toolCallId, String toolName) {

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, List.of(), null, null);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content, List.of(), null, null);
    }

    public static ChatMessage assistantCalling(List<ToolCall> calls) {
        return new ChatMessage("assistant", "", calls, null, null);
    }

    public static ChatMessage toolResult(ToolCall call, String content) {
        return new ChatMessage("tool", content, List.of(), call.id(), call.name());
    }
}
