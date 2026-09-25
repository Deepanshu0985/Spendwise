package com.finance.infrastructure.web.analytics;

import com.finance.domain.analytics.CurrencyExclusion;

import java.util.List;

/** meta shape for every analytics endpoint (analytics-specification.md's Currency Scoping - silent exclusion is forbidden). */
public record AnalyticsMeta(String currency, List<String> excludedCurrencies, long excludedTransactionCount) {

    public static AnalyticsMeta of(String currency, CurrencyExclusion exclusion) {
        return new AnalyticsMeta(currency, exclusion.excludedCurrencies(), exclusion.excludedTransactionCount());
    }
}
