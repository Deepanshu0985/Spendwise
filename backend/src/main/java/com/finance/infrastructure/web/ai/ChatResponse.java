package com.finance.infrastructure.web.ai;

import com.finance.application.assistant.AssistantReply;

import java.util.List;

public record ChatResponse(String answer, List<ToolUse> toolsUsed, boolean fallback, int remainingMessagesToday) {

    public record ToolUse(String name, String context) {
    }

    public static ChatResponse from(AssistantReply reply) {
        return new ChatResponse(
                reply.answer(), reply.toolsUsed().stream().map(t -> new ToolUse(t.name(), t.context())).toList(), reply.fallback(),
                reply.remainingMessagesToday());
    }
}
