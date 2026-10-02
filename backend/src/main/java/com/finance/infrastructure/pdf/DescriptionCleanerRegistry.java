package com.finance.infrastructure.pdf;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Picks the DescriptionCleaner for a statement's bank, mirroring StatementFormatDetector:
 * Spring injects every DescriptionCleaner @Component, so supporting another bank means
 * adding one class. A bank with no cleaner, or a narration its cleaner doesn't recognise,
 * keeps the fallback description unchanged.
 */
@Component
public class DescriptionCleanerRegistry {

    private final List<DescriptionCleaner> cleaners;

    public DescriptionCleanerRegistry(List<DescriptionCleaner> cleaners) {
        this.cleaners = cleaners;
    }

    public String clean(String bankName, String rawDescription, String fallback) {
        return cleaners.stream()
                .filter(cleaner -> cleaner.bankName().equals(bankName))
                .findFirst()
                .flatMap(cleaner -> cleaner.clean(rawDescription))
                .orElse(fallback);
    }
}
