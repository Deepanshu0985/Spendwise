package com.finance.domain.statement;

import com.finance.domain.transaction.Transaction;

import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Framework-free scoring per duplicate-detection.md: exact reference is strongest, then
 * same date + amount + similar description, then nearby date + similar description.
 * Candidates already consumed by an earlier row are skipped, so two genuinely identical
 * purchases on one day are not both collapsed into a single imported transaction.
 */
public final class DuplicateScorer {

    public static final int NEARBY_DAYS = 2;
    static final double HIGH_SIMILARITY = 0.8;
    static final double MEDIUM_SIMILARITY = 0.5;

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Set<String> NOISE_TOKENS = Set.of("upi", "paid", "to", "from", "received", "money", "sent", "neft", "imps", "pos");

    private DuplicateScorer() {
    }

    /**
     * @param accountId the account the staged row belongs to; reference matches ignore it (a UPI RRN
     *                  is unique across accounts), description/date matches require the same account.
     */
    public static Optional<DuplicateMatch> score(
            StatementTransaction row, UUID accountId, List<Transaction> candidates, Set<UUID> alreadyMatched) {
        DuplicateMatch best = null;
        for (Transaction candidate : candidates) {
            if (alreadyMatched.contains(candidate.getId()) || candidate.getAmount().compareTo(row.getAmount()) != 0) {
                continue;
            }
            DuplicateMatch match = matchOne(row, accountId, candidate);
            if (match != null && (best == null || rank(match.status()) > rank(best.status()))) {
                best = match;
            }
        }
        return Optional.ofNullable(best);
    }

    private static DuplicateMatch matchOne(StatementTransaction row, UUID accountId, Transaction candidate) {
        String reference = row.getExternalReference();
        if (reference != null && !reference.isBlank() && reference.equals(candidate.getExternalTransactionId())) {
            return new DuplicateMatch(DuplicateStatus.DUPLICATE, DuplicateReason.EXACT_REFERENCE, candidate.getId());
        }
        if (!accountId.equals(candidate.getAccountId())) {
            return null;
        }
        long dayGap = Math.abs(ChronoUnit.DAYS.between(row.getTransactionDate(), candidate.getTransactionDate()));
        if (dayGap > NEARBY_DAYS) {
            return null;
        }
        double similarity = similarity(row.getRawDescription(), candidate.getRawDescription() != null
                ? candidate.getRawDescription() : candidate.getDescription());
        if (dayGap == 0 && similarity >= HIGH_SIMILARITY) {
            return new DuplicateMatch(DuplicateStatus.DUPLICATE, DuplicateReason.DATE_AMOUNT_DESCRIPTION, candidate.getId());
        }
        if (similarity >= MEDIUM_SIMILARITY) {
            return new DuplicateMatch(DuplicateStatus.POSSIBLE_DUPLICATE, DuplicateReason.NEARBY_SIMILAR, candidate.getId());
        }
        return null;
    }

    private static int rank(DuplicateStatus status) {
        return status == DuplicateStatus.DUPLICATE ? 2 : 1;
    }

    /** Jaccard similarity over alphanumeric tokens, ignoring payment-prefix noise words and long digit runs (references). */
    static double similarity(String a, String b) {
        Set<String> left = tokens(a);
        Set<String> right = tokens(b);
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        return (double) intersection.size() / union.size();
    }

    private static Set<String> tokens(String text) {
        Set<String> tokens = new HashSet<>();
        if (text == null) {
            return tokens;
        }
        for (String token : NON_ALPHANUMERIC.split(text.toLowerCase(Locale.ROOT))) {
            if (!token.isBlank() && !NOISE_TOKENS.contains(token) && !token.matches("\\d{8,}")) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
