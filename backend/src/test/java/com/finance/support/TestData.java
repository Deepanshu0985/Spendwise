package com.finance.support;

import java.util.UUID;

/** Randomized test data so repeated local runs against a shared database (e.g. Neon dev) don't collide on unique constraints. */
public final class TestData {

    private TestData() {
    }

    public static String uniqueEmail(String label) {
        return label + "-" + UUID.randomUUID() + "@example.com";
    }
}
