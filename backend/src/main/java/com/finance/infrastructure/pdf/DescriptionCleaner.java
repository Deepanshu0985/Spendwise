package com.finance.infrastructure.pdf;

import java.util.Optional;

/**
 * One implementation per bank/wallet whose narration needs bank-specific cleanup
 * (UPI reference numbers, timestamps and bank-handle suffixes stripped down to what
 * the user would recognise). Only the display description is touched - classification
 * still runs against the raw description (see TransactionNormalizer), and the raw text
 * is always kept alongside for audit.
 */
public interface DescriptionCleaner {

    /** Matches StatementParser.bankName() of the bank this cleaner handles. */
    String bankName();

    /** The cleaned description, or empty when this narration has no recognised pattern to clean. */
    Optional<String> clean(String rawDescription);
}
