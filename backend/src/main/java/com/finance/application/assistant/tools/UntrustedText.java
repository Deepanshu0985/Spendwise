package com.finance.application.assistant.tools;

import java.util.regex.Pattern;

/**
 * Names in tool results (merchants, categories, budgets, goals) are text someone chose - a payee can pick any name. The
 * delimiter around tool data already tells the model not to obey it, but a model may still react by silently dropping the
 * row. So a name that reads like an instruction is replaced with a placeholder before the model sees it: the row and its
 * figure are still reported, and the instruction never reaches the model at all. This is a second layer, a heuristic -
 * the real controls are the read-only tools, the session-derived identity and the grounding gate.
 */
final class UntrustedText {

    static final String HIDDEN = "[name hidden: it reads like an instruction]";
    private static final int MAX_LENGTH = 80;

    private static final Pattern CONTROL = Pattern.compile("\\p{Cntrl}");
    private static final Pattern LOOKS_LIKE_AN_INSTRUCTION = Pattern.compile(
            "(?i)\\b(?:ignore|disregard|forget|override|bypass)\\b.{0,50}\\b(?:instruction|rule|prompt|previous|above|earlier|polic)"
                    + "|\\b(?:system|assistant|developer|admin)\\s*(?::|prompt|message|override)"
                    + "|\\b(?:reveal|show|print|leak|tell me)\\b.{0,40}\\b(?:prompt|instruction|other users?|system)"
                    + "|\\bnew\\s+instructions?\\b|\\byou\\s+are\\s+now\\b|\\bact\\s+as\\b|\\bcall\\s+(?:the\\s+)?[a-z_]+_(?:status|summary|spending|merchants|trend|expenses)\\b"
                    + "|<<<|>>>");

    private UntrustedText() {
    }

    static String of(String name) {
        if (name == null) {
            return "";
        }
        String text = CONTROL.matcher(name).replaceAll(" ").trim();
        if (LOOKS_LIKE_AN_INSTRUCTION.matcher(text).find()) {
            return HIDDEN;
        }
        return text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) + "…" : text;
    }
}
