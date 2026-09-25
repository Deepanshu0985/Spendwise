package com.finance.application.analytics;

import com.finance.domain.analytics.CurrencyExclusion;
import com.finance.domain.analytics.MerchantBreakdownEntry;

import java.util.List;

public record MerchantBreakdownView(String currency, List<MerchantBreakdownEntry> entries, CurrencyExclusion exclusion) {
}
