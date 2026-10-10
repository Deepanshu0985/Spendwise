package com.finance.domain.ai;

import java.util.regex.Pattern;

/**
 * Removes what a model does not need to categorise a payment and that could identify someone: long digit runs (phone,
 * account, card, reference numbers), and the bank part of a UPI id or email (the part before the @ is kept - it often names
 * the merchant). Applied to every description before it leaves the application.
 */
public final class PiiRedactor {

    private static final Pattern HANDLE_DOMAIN = Pattern.compile("(?<=[A-Za-z0-9._-])@[A-Za-z0-9.-]+");
    // Digits possibly broken up by spaces or dashes: "98765 43210", "4111-1111-1111-1111".
    private static final Pattern DIGIT_RUN = Pattern.compile("\\d(?:[ -]?\\d){5,}");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^ ]]");
    private static final int MAX_LENGTH = 200;

    private PiiRedactor() {
    }

    public static String redact(String description) {
        if (description == null) {
            return "";
        }
        String text = CONTROL_CHARS.matcher(description).replaceAll(" ");
        text = HANDLE_DOMAIN.matcher(text).replaceAll("");
        text = DIGIT_RUN.matcher(text).replaceAll("[number]");
        text = text.replaceAll("\\s+", " ").trim();
        return text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text;
    }
}
