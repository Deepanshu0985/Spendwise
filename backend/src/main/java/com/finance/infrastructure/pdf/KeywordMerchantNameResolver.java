package com.finance.infrastructure.pdf;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Free, offline merchant recognition: well-known brands whose name appears inside the UPI
 * handle ("zomatoltd32.rzp", "blinkit.payu", "payzomato"). Keywords are deliberately long
 * and distinctive so a short word can't match inside an unrelated handle. Handles that
 * name a person or a generic QR merchant stay unresolved on purpose.
 */
@Component
public class KeywordMerchantNameResolver implements MerchantNameResolver {

    private static final Map<String, String> BRANDS = new LinkedHashMap<>();

    static {
        BRANDS.put("zomato", "Zomato");
        BRANDS.put("swiggy", "Swiggy");
        BRANDS.put("zepto", "Zepto");
        BRANDS.put("blinkit", "Blinkit");
        BRANDS.put("bigbasket", "BigBasket");
        BRANDS.put("instamart", "Swiggy Instamart");
        BRANDS.put("amazon", "Amazon");
        BRANDS.put("flipkart", "Flipkart");
        BRANDS.put("myntra", "Myntra");
        BRANDS.put("airtel", "Airtel");
        BRANDS.put("appleservices", "Apple");
        BRANDS.put("netflix", "Netflix");
        BRANDS.put("spotify", "Spotify");
        BRANDS.put("irctc", "IRCTC");
        BRANDS.put("snapmint", "Snapmint");
        BRANDS.put("delhivery", "Delhivery");
    }

    @Override
    public Optional<String> resolve(String handle) {
        String lower = handle.toLowerCase(Locale.ROOT);
        return BRANDS.entrySet().stream()
                .filter(brand -> lower.contains(brand.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }
}
