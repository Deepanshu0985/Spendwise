package com.finance.infrastructure.web.merchant;

import com.finance.domain.merchant.Merchant;

import java.util.UUID;

public record MerchantResponse(UUID id, String canonicalName, String normalizedKey) {

    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(merchant.getId(), merchant.getCanonicalName(), merchant.getNormalizedKey());
    }
}
