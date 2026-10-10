package com.finance.domain.ai;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The free layer that runs before any model: well-known brands whose category is not in doubt. Keywords are long and
 * distinctive on purpose so a short word cannot match inside an unrelated UPI handle. Names are those of the system
 * categories; a user who removed or renamed one simply gets no match.
 */
public final class KeywordCategoryRules {

    private static final Map<String, String> RULES = new LinkedHashMap<>();

    static {
        // More specific brands first: "Swiggy Instamart" is groceries, plain Swiggy is food.
        RULES.put("instamart", "Groceries");
        RULES.put("zomato", "Food & Dining");
        RULES.put("swiggy", "Food & Dining");
        RULES.put("zepto", "Groceries");
        RULES.put("blinkit", "Groceries");
        RULES.put("bigbasket", "Groceries");
        RULES.put("amazon", "Shopping");
        RULES.put("flipkart", "Shopping");
        RULES.put("myntra", "Shopping");
        RULES.put("netflix", "Subscriptions");
        RULES.put("spotify", "Subscriptions");
        RULES.put("hotstar", "Subscriptions");
        RULES.put("appleservices", "Subscriptions");
        RULES.put("bookmyshow", "Entertainment");
        RULES.put("irctc", "Travel");
        RULES.put("makemytrip", "Travel");
        RULES.put("airtel", "Bills & Utilities");
        RULES.put("vodafone", "Bills & Utilities");
        RULES.put("bsnl", "Bills & Utilities");
        RULES.put("uber", "Transportation");
    }

    private KeywordCategoryRules() {
    }

    public static Optional<String> categoryNameFor(String... texts) {
        for (String text : texts) {
            if (text == null) {
                continue;
            }
            String lower = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            for (Map.Entry<String, String> rule : RULES.entrySet()) {
                if (lower.contains(rule.getKey())) {
                    return Optional.of(rule.getValue());
                }
            }
        }
        return Optional.empty();
    }
}
