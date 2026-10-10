package com.finance.application.assistant;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The grounding gate (ai-evaluation.md): every monetary figure in an answer must be a value a tool returned in that same
 * exchange. Amounts with a currency marker, percentages, and any number written with a comma, a decimal point or four or
 * more digits are checked; dates, years, list numbers and small counts are not. Only values that were JSON *numbers* in
 * a tool result count as returned - digits inside dates or names do not make a figure legitimate.
 */
public final class GroundingChecker {

    private static final String MONTH = "(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\.?";
    private static final Pattern DATE_LIKE = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}"
                    + "|\\b\\d{1,2}(?:st|nd|rd|th)?\\s+" + MONTH + "(?:,?\\s+\\d{4})?"
                    // "Oct 5" is a date, but "September 19,249" is a month followed by an amount: a day is not followed by more digits.
                    + "|\\b" + MONTH + "\\s+\\d{1,2}(?:st|nd|rd|th)?(?!\\d)(?!,\\d{3})(?!\\.\\d)(?:,?\\s+\\d{4})?"
                    + "|\\b" + MONTH + "\\s+\\d{4}",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LIST_MARKER = Pattern.compile("(?m)^\\s*\\d+[.)]\\s");
    // A figure may carry its currency before ("₹500", "INR 500") or after ("500 INR", "500 rupees"), or be a percentage.
    private static final Pattern NUMBER = Pattern.compile(
            "(?<![\\w.])(₹|Rs\\.?|INR|USD|\\$)?\\s?(\\d{1,3}(?:,\\d{2,3})+(?:\\.\\d+)?|\\d+(?:\\.\\d+)?)(?:(\\s?%)|(\\s?(?:INR|USD|Rs\\b\\.?|rupees?|rs\\b)))?",
            Pattern.CASE_INSENSITIVE);
    private static final BigDecimal HALF = new BigDecimal("0.5");

    private GroundingChecker() {
    }

    /** Collects every JSON number found anywhere in the given tool results. */
    public static Set<BigDecimal> numbersIn(Collection<JsonNode> toolResults) {
        Set<BigDecimal> numbers = new HashSet<>();
        for (JsonNode result : toolResults) {
            collect(result, numbers);
        }
        return numbers;
    }

    private static void collect(JsonNode node, Set<BigDecimal> out) {
        if (node.isNumber()) {
            out.add(node.decimalValue().abs());
        } else if (node.isContainerNode()) {
            node.forEach(child -> collect(child, out));
        }
    }

    /** The figures in the answer that no tool returned, as written; empty when the answer is grounded. */
    public static List<String> ungrounded(String answer, Set<BigDecimal> allowed) {
        String text = LIST_MARKER.matcher(DATE_LIKE.matcher(answer).replaceAll(" ")).replaceAll(" ");
        List<String> bad = new ArrayList<>();
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            String raw = m.group(2);
            boolean marked = m.group(1) != null || m.group(3) != null || m.group(4) != null;
            boolean formatted = raw.contains(",") || raw.contains(".");
            String digits = raw.replace(",", "").split("\\.")[0];
            boolean yearLike = !raw.contains(",") && !raw.contains(".") && digits.length() == 4
                    && Integer.parseInt(digits) >= 1900 && Integer.parseInt(digits) <= 2100;
            boolean checkable = marked || formatted || (digits.length() >= 4 && !yearLike);
            if (!checkable) {
                continue;
            }
            BigDecimal value = new BigDecimal(raw.replace(",", ""));
            if (!isAllowed(value, raw.contains("."), allowed)) {
                bad.add(m.group(0).trim());
            }
        }
        return bad;
    }

    private static boolean isAllowed(BigDecimal value, boolean writtenWithDecimals, Set<BigDecimal> allowed) {
        for (BigDecimal a : allowed) {
            if (a.compareTo(value) == 0) {
                return true;
            }
            // A whole number may be a tool value honestly rounded to the nearest unit.
            if (!writtenWithDecimals && a.subtract(value).abs().compareTo(HALF) <= 0) {
                return true;
            }
        }
        return false;
    }
}
