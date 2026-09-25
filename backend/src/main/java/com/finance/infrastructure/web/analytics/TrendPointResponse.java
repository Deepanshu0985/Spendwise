package com.finance.infrastructure.web.analytics;

import com.finance.domain.analytics.TrendPoint;

import java.math.BigDecimal;

public record TrendPointResponse(String month, BigDecimal expenses, BigDecimal income, BigDecimal savings, BigDecimal savingsRate) {

    public static TrendPointResponse from(TrendPoint point) {
        return new TrendPointResponse(
                point.month().toString(), point.figures().expenses(), point.figures().income(), point.figures().savings(), point.figures().savingsRate());
    }
}
