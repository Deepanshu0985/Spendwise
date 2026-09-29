package com.finance.infrastructure.pdf;

import com.finance.domain.statement.ParsedTransactionRow;
import com.finance.domain.transaction.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Framework-free, deterministic - no I/O, no Spring context needed to test it in
 * isolation (same shape as domain.analytics.AnalyticsCalculator). Implements
 * transaction-normalization.md's rules and classification table.
 *
 * Full transfer-pairing (matching a same-side row against the user's other
 * tracked accounts, per the doc's "Transfer Pairing" section) is an
 * application-layer concern working across accounts, not something this
 * framework-free class can do alone - it classifies TRANSFER_OUT/TRANSFER_IN
 * and CARD_PAYMENT_OUT/CARD_PAYMENT_IN from narration keywords only; actually
 * locating/creating the counterpart transaction happens in StatementServiceImpl.
 */
@Component
public class TransactionNormalizer {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern PAYMENT_PREFIX =
            Pattern.compile("^(UPI|POS|NEFT|IMPS|ATM)[/\\-\\s]+", Pattern.CASE_INSENSITIVE);

    private static final BigDecimal HIGH_CONFIDENCE = new BigDecimal("0.85");
    private static final BigDecimal MEDIUM_CONFIDENCE = new BigDecimal("0.75");
    private static final BigDecimal LOW_CONFIDENCE = new BigDecimal("0.60");

    public NormalizedTransactionRow normalize(ParsedTransactionRow row) {
        String normalizedDescription = normalizeDescription(row.rawDescription());
        boolean isDebit = row.debitCredit() == ParsedTransactionRow.DebitCredit.DEBIT;
        // Classify against the raw description, not the normalized one: normalizeDescription()
        // strips exactly the ATM/UPI/POS/NEFT/IMPS prefixes rule 7 asks for, but those same
        // words are often the classification signal itself (e.g. "ATM WDL CASH") - stripping
        // first and classifying after would throw away the evidence.
        String upper = row.rawDescription().toUpperCase();

        if (isDebit && upper.contains("ATM")) {
            return new NormalizedTransactionRow(normalizedDescription, TransactionType.CASH_WITHDRAWAL, HIGH_CONFIDENCE);
        }
        if (!isDebit && upper.contains("SALARY")) {
            return new NormalizedTransactionRow(normalizedDescription, TransactionType.INCOME, HIGH_CONFIDENCE);
        }
        if (!isDebit && (upper.contains("REVERSAL") || upper.contains("REFUND"))) {
            return new NormalizedTransactionRow(normalizedDescription, TransactionType.REFUND, HIGH_CONFIDENCE);
        }
        if (isDebit && upper.contains("INTEREST")) {
            return new NormalizedTransactionRow(normalizedDescription, TransactionType.INTEREST_CHARGED, HIGH_CONFIDENCE);
        }
        if (!isDebit && upper.contains("INTEREST")) {
            return new NormalizedTransactionRow(normalizedDescription, TransactionType.INTEREST_EARNED, HIGH_CONFIDENCE);
        }
        if (isDebit && (upper.contains("FEE") || upper.contains("CHARGE"))) {
            return new NormalizedTransactionRow(normalizedDescription, TransactionType.FEE_CHARGED, HIGH_CONFIDENCE);
        }
        if (upper.contains("CARD PAYMENT") || upper.contains("CC PAYMENT") || upper.contains("CARD BILL")) {
            TransactionType type = isDebit ? TransactionType.CARD_PAYMENT_OUT : TransactionType.CARD_PAYMENT_IN;
            return new NormalizedTransactionRow(normalizedDescription, type, MEDIUM_CONFIDENCE);
        }
        if (upper.contains("SELF") || upper.contains("OWN A/C") || upper.contains("OWN ACCOUNT")) {
            TransactionType type = isDebit ? TransactionType.TRANSFER_OUT : TransactionType.TRANSFER_IN;
            return new NormalizedTransactionRow(normalizedDescription, type, MEDIUM_CONFIDENCE);
        }

        // Rule from the classification table: an otherwise-unmatched debit is a
        // plain merchant expense, an otherwise-unmatched credit is generic income -
        // reviewable, not guessed away as UNKNOWN, which is reserved for rows that
        // don't fit the model at all (StatementServiceImpl, not this normalizer).
        TransactionType fallback = isDebit ? TransactionType.EXPENSE : TransactionType.INCOME;
        return new NormalizedTransactionRow(normalizedDescription, fallback, LOW_CONFIDENCE);
    }

    private String normalizeDescription(String rawDescription) {
        String collapsed = WHITESPACE.matcher(rawDescription.trim()).replaceAll(" ");
        return PAYMENT_PREFIX.matcher(collapsed).replaceFirst("");
    }
}
