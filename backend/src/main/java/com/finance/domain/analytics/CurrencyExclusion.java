package com.finance.domain.analytics;

import java.util.List;

/**
 * What was left out of a report because it wasn't in the requested currency -
 * silent exclusion is forbidden (analytics-specification.md's Currency Scoping),
 * so every analytics response surfaces this alongside its figures.
 */
public record CurrencyExclusion(List<String> excludedCurrencies, long excludedTransactionCount) {

    public static final CurrencyExclusion NONE = new CurrencyExclusion(List.of(), 0);
}
