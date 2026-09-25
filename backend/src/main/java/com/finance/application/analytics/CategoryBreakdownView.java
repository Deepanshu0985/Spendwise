package com.finance.application.analytics;

import com.finance.domain.analytics.CategoryBreakdownEntry;
import com.finance.domain.analytics.CurrencyExclusion;

import java.util.List;

public record CategoryBreakdownView(String currency, List<CategoryBreakdownEntry> entries, CurrencyExclusion exclusion) {
}
