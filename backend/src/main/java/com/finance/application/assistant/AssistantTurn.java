package com.finance.application.assistant;

/** One earlier turn of the conversation as the browser sent it: role is "user" or "assistant". Nothing is stored server-side. */
public record AssistantTurn(String role, String content) {
}
