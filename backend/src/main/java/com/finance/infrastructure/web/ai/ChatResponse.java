package com.finance.infrastructure.web.ai;

import com.finance.application.assistant.AssistantReply;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ChatResponse(String answer, List<ToolUse> toolsUsed, List<Source> sources, boolean fallback, int remainingMessagesToday) {

    public record ToolUse(String name, String context) {
    }

    /** A transaction the answer looked at. The record id is deliberately not exposed: the model never saw it either. */
    public record Source(LocalDate date, String description, String merchant, BigDecimal amount, String currency, String type) {
    }

    public static ChatResponse from(AssistantReply reply) {
        return new ChatResponse(
                reply.answer(), reply.toolsUsed().stream().map(t -> new ToolUse(t.name(), t.context())).toList(),
                reply.sources().stream().map(s -> new Source(s.date(), s.description(), s.merchant(), s.amount(), s.currency(), s.type())).toList(),
                reply.fallback(), reply.remainingMessagesToday());
    }
}
