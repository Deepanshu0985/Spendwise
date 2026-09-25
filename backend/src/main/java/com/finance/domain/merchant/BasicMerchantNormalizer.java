package com.finance.domain.merchant;

import org.springframework.stereotype.Component;

/** Pure algorithmic domain service - no persistence/web I/O, so it lives directly in the domain layer rather than needing an infrastructure adapter. */
@Component
public class BasicMerchantNormalizer implements MerchantNormalizer {

    @Override
    public String normalize(String rawName) {
        return rawName
                .strip()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .strip();
    }
}
