package com.finance.application.assistant;

import java.util.Locale;

/**
 * Typo tolerance for the search tools, done in the application so no database extension is needed: when nothing matches
 * exactly, a word counts as close to a text if it is within a small edit distance of one of the text's words.
 */
public final class FuzzyMatcher {

    private FuzzyMatcher() {
    }

    /** True if some word of the text is close enough to the query word. Short words must match exactly. */
    public static boolean isClose(String queryWord, String text) {
        String query = queryWord.toLowerCase(Locale.ROOT);
        if (query.length() < 4) {
            return false;
        }
        int allowedEdits = query.length() >= 8 ? 2 : 1;
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{Nd}]+")) {
            if (word.isEmpty() || Math.abs(word.length() - query.length()) > allowedEdits) {
                continue;
            }
            if (editDistance(query, word) <= allowedEdits) {
                return true;
            }
        }
        return false;
    }

    static int editDistance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
