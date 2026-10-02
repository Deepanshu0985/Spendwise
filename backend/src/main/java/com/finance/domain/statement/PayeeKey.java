package com.finance.domain.statement;

import java.util.Locale;
import java.util.regex.Pattern;

/** The stable lookup key a learned rule is stored under: the display description reduced to lowercase alphanumeric words. */
public final class PayeeKey {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private PayeeKey() {
    }

    public static String of(String description) {
        if (description == null) {
            return "";
        }
        return NON_ALPHANUMERIC.matcher(description.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }
}
