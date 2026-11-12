package com.finance.infrastructure.pdf;

import org.springframework.stereotype.Component;

import java.util.Optional;

/** The bob World app writes the same UPI narrations as the older Bank of Baroda export, so it cleans them the same way. */
@Component
public class BobWorldDescriptionCleaner implements DescriptionCleaner {

    private final BobDescriptionCleaner bobCleaner;

    public BobWorldDescriptionCleaner(BobDescriptionCleaner bobCleaner) {
        this.bobCleaner = bobCleaner;
    }

    @Override
    public String bankName() {
        return "BOB_WORLD";
    }

    @Override
    public Optional<String> clean(String rawDescription) {
        return bobCleaner.clean(rawDescription);
    }
}
