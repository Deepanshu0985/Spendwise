package com.finance.merchant;

/**
 * Basic stub for V1 (phase-plan.md's Phase 2 scope). The real duplicate-scoring
 * normalization (fuzzy matching, payment-prefix stripping) is Phase 7's job,
 * once statement import gives it real data to tune against - see
 * 05-statement-processing/duplicate-detection.md.
 */
public interface MerchantNormalizer {

    String normalize(String rawName);
}
