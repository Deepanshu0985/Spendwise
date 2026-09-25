package com.finance.application.analytics;

import com.finance.domain.analytics.CurrencyExclusion;
import com.finance.domain.analytics.TrendPoint;

import java.util.List;

public record TrendView(String currency, List<TrendPoint> points, CurrencyExclusion exclusion) {
}
