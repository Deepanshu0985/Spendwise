package com.finance.application.assistant;

import java.util.List;
import java.util.UUID;

public interface AssistantService {

    /**
     * Answers the last user turn using only the allowlisted read-only tools, run as the given (session) user. The
     * conversation is whatever the caller sends; nothing is stored. Every figure in the reply was returned by a tool.
     *
     * @throws com.finance.application.exception.AiUnavailableException  AI is off or the model could not be reached
     * @throws com.finance.application.exception.AiQuotaExceededException a usage cap is used up
     */
    AssistantReply chat(UUID userId, List<AssistantTurn> conversation);
}
