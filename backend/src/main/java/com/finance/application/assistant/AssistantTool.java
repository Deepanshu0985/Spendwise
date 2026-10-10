package com.finance.application.assistant;

import com.finance.application.ai.ToolSpec;

import java.util.UUID;

/**
 * One allowlisted, read-only finance tool (ai-tools.md). The signature is the security rule: there is a userId from the
 * authenticated session and the model's arguments, and the arguments are never given the chance to name a user. Results
 * come from the same services that produce the numbers on screen.
 */
public interface AssistantTool {

    ToolSpec spec();

    /**
     * @param userId        taken from the session by the caller, never from the model
     * @param argumentsJson what the model supplied, untrusted
     * @throws ToolArgumentException if the arguments fail validation
     */
    ToolResult execute(UUID userId, String argumentsJson);
}
