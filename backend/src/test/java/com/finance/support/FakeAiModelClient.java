package com.finance.support;

import com.finance.application.ai.AiModelClient;
import com.finance.application.ai.ChatMessage;
import com.finance.application.ai.ChatResult;
import com.finance.application.ai.ModelResult;
import com.finance.application.ai.ToolSpec;
import com.finance.application.exception.AiUnavailableException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A scripted stand-in for the model: no network, no credits. Records every prompt so tests can inspect what would be sent. */
public class FakeAiModelClient implements AiModelClient {

    public record Answer(String type, String category, double confidence, String reason) {
    }

    private static final Pattern ROW = Pattern.compile("^(\\d+) \\| (debit|credit) \\| (.*)$");

    public volatile boolean available = true;
    public volatile boolean failing = false;
    public volatile Function<String, String> responder = content -> "{\"results\":[]}";
    public final List<String> systemPrompts = new ArrayList<>();
    public final List<String> userContents = new ArrayList<>();

    /** Scripted assistant: given the conversation so far, returns the model's next turn. Records every call for inspection. */
    public volatile Function<List<ChatMessage>, ChatResult> chatResponder = messages -> new ChatResult("(no script)", List.of(), 10, 5);
    public final List<List<ChatMessage>> chatCalls = new ArrayList<>();
    public final List<String> chatSystemPrompts = new ArrayList<>();
    public volatile List<ToolSpec> lastTools = List.of();

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public synchronized ModelResult complete(String systemPrompt, String userContent) {
        systemPrompts.add(systemPrompt);
        userContents.add(userContent);
        if (failing) {
            throw new AiUnavailableException("simulated outage");
        }
        return new ModelResult(responder.apply(userContent), 100, 50);
    }

    @Override
    public synchronized ChatResult chat(String systemPrompt, List<ChatMessage> messages, List<ToolSpec> tools) {
        chatSystemPrompts.add(systemPrompt);
        chatCalls.add(new ArrayList<>(messages));
        lastTools = tools;
        if (failing) {
            throw new AiUnavailableException("simulated outage");
        }
        return chatResponder.apply(messages);
    }

    public synchronized void reset() {
        available = true;
        failing = false;
        chatResponder = messages -> new ChatResult("(no script)", List.of(), 10, 5);
        chatCalls.clear();
        chatSystemPrompts.clear();
        lastTools = List.of();
        responder = content -> "{\"results\":[]}";
        systemPrompts.clear();
        userContents.clear();
    }

    /** Answers each numbered row whose text contains a key; rows matching no key get no answer. */
    public static Function<String, String> byKeyword(Map<String, Answer> answers) {
        return content -> {
            StringBuilder out = new StringBuilder("{\"results\":[");
            boolean first = true;
            for (String line : content.split("\n")) {
                Matcher m = ROW.matcher(line);
                if (!m.matches()) {
                    continue;
                }
                for (Map.Entry<String, Answer> entry : answers.entrySet()) {
                    if (m.group(3).toLowerCase().contains(entry.getKey().toLowerCase())) {
                        Answer a = entry.getValue();
                        out.append(first ? "" : ",").append(String.format(
                                "{\"index\":%s,\"transactionType\":\"%s\",\"category\":%s,\"confidence\":%s,\"reason\":\"%s\"}",
                                m.group(1), a.type(), a.category() == null ? "null" : "\"" + a.category() + "\"", a.confidence(), a.reason()));
                        first = false;
                        break;
                    }
                }
            }
            return out.append("]}").toString();
        };
    }
}
