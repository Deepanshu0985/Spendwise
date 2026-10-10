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

    /** Every number written in a piece of text (the user's own question), commas allowed. */
    public static Set<BigDecimal> numbersInText(String text) {
        Set<BigDecimal> numbers = new HashSet<>();
        Matcher m = Pattern.compile("\\d{1,3}(?:,\\d{2,3})+(?:\\.\\d+)?|\\d+(?:\\.\\d+)?").matcher(text == null ? "" : text);
        while (m.find()) {
            numbers.add(new BigDecimal(m.group().replace(",", "")));
        }
        return numbers;
    }

    /**
     * Digit runs that appear inside text values of tool results (a reference number or invoice number in a quoted
     * description). Quoting such a number is legitimate; it never authorises an amount (a figure with a currency or a percent sign).
     */
    public static Set<String> textNumbersIn(Collection<JsonNode> toolResults) {
        Set<String> numbers = new HashSet<>();
        for (JsonNode result : toolResults) {
            collectText(result, numbers);
        }
        return numbers;
    }

    private static void collectText(JsonNode node, Set<String> out) {
        if (node.isTextual()) {
            Matcher m = Pattern.compile("\\d+").matcher(node.asText());
            while (m.find()) {
                out.add(m.group());
            }
        } else if (node.isContainerNode()) {
            node.forEach(child -> collectText(child, out));
        }
    }

    /** The figures in the answer that no tool returned, as written; empty when the answer is grounded. */
    public static List<String> ungrounded(String answer, Set<BigDecimal> allowed) {
        return ungrounded(answer, allowed, Set.of());
    }

    public static List<String> ungrounded(String answer, Set<BigDecimal> allowed, Set<String> allowedQuotedDigits) {
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
            boolean quotedText = !marked && !raw.contains(".") && allowedQuotedDigits.contains(raw.replace(",", ""));
            if (!quotedText && !isAllowed(value, raw.contains("."), allowed)) {
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
