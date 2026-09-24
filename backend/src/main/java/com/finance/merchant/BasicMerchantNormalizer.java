package com.finance.merchant;

import org.springframework.stereotype.Component;

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
