package com.finance.application.statement;

import com.finance.domain.statement.DuplicateMatch;
import com.finance.domain.statement.DuplicateScorer;
import com.finance.domain.statement.ReviewStatus;
import com.finance.domain.statement.StatementTransaction;
import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DuplicateDetectionServiceImpl implements DuplicateDetectionService {

    private final TransactionRepository transactionRepository;

    public DuplicateDetectionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void score(UUID userId, UUID accountId, List<StatementTransaction> rows) {
        List<StatementTransaction> toScore = rows.stream().filter(row -> row.getReviewStatus() != ReviewStatus.REJECTED).toList();
        if (toScore.isEmpty()) {
            return;
        }
        LocalDate earliest = toScore.stream().map(StatementTransaction::getTransactionDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate latest = toScore.stream().map(StatementTransaction::getTransactionDate).max(LocalDate::compareTo).orElseThrow();
        Set<String> references = toScore.stream()
                .map(StatementTransaction::getExternalReference)
                .filter(reference -> reference != null && !reference.isBlank())
                .collect(Collectors.toSet());

        List<Transaction> candidates = transactionRepository.findDuplicateCandidates(
                userId, earliest.minusDays(DuplicateScorer.NEARBY_DAYS), latest.plusDays(DuplicateScorer.NEARBY_DAYS), references);

        Set<UUID> alreadyMatched = new HashSet<>();
        for (StatementTransaction row : toScore) {
            DuplicateMatch match = DuplicateScorer.score(row, accountId, candidates, alreadyMatched).orElse(null);
            row.applyDuplicateScore(match);
            if (match != null) {
                alreadyMatched.add(match.matchedTransactionId());
            }
        }
    }
}
