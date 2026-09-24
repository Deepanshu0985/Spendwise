package com.finance.merchant;

import java.util.UUID;

public record MerchantResponse(UUID id, String canonicalName, String normalizedKey) {

    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(merchant.getId(), merchant.getCanonicalName(), merchant.getNormalizedKey());
    }
}
