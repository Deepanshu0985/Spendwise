package com.finance.infrastructure.pdf;

import java.util.Optional;

/**
 * Turns a cleaned payee handle (e.g. "zomatoltd32.rzp") into a recognisable merchant name.
 * Behind an interface so a free built-in dictionary today can be joined or replaced by a
 * learned/AI-backed resolver later without touching the per-bank cleaners.
 */
public interface MerchantNameResolver {

    Optional<String> resolve(String handle);
}
