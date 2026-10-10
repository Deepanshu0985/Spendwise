package com.finance.application.ai;

/**
 * The single egress point to a language model (phase plan: "AIModelClient egress chokepoint"). Nothing else in the
 * application may talk to a model provider, so redaction, caps and logging policy live in one place and the provider
 * can be swapped without touching callers.
 */
public interface AiModelClient {

    /** The model that answers, recorded on anything it writes so the text stays attributable. */
    String modelName();

    /** False when AI is switched off or no credentials are configured - callers then skip the model entirely. */
    boolean isAvailable();

    /**
     * @param systemPrompt trusted instructions written by this application
     * @param userContent  the request, including any attacker-influenced text, already delimited as data by the caller
     * @throws com.finance.application.exception.AiUnavailableException when the model cannot be reached or answers with an error
     */
    ModelResult complete(String systemPrompt, String userContent);

    /**
     * A conversation turn with optional tools (the assistant). The same rules as complete(): this is the only way to reach
     * the model, and tool calls come back as data for the caller to validate and run - the model never executes anything.
     *
     * @throws com.finance.application.exception.AiUnavailableException when the model cannot be reached or answers with an error
     */
    ChatResult chat(String systemPrompt, java.util.List<ChatMessage> messages, java.util.List<ToolSpec> tools);
}
