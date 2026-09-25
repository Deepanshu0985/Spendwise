package com.finance.infrastructure.web.analytics;

import com.finance.domain.analytics.MerchantBreakdownEntry;

import java.math.BigDecimal;
import java.util.UUID;

public record MerchantEntryResponse(UUID merchantId, String merchantName, BigDecimal amount) {

    public static MerchantEntryResponse from(MerchantBreakdownEntry entry) {
        return new MerchantEntryResponse(entry.merchantId(), entry.merchantName(), entry.amount());
    }
}
