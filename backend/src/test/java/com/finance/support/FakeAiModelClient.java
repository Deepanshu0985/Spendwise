package com.finance.support;

import com.finance.application.ai.AiModelClient;
import com.finance.application.ai.ModelResult;
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

    public synchronized void reset() {
        available = true;
        failing = false;
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
