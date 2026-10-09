package com.finance.application.recurring;

import com.finance.application.exception.NotFoundException;
import com.finance.domain.category.CategoryRepository;
import com.finance.domain.merchant.MerchantRepository;
import com.finance.domain.recurring.RecurrenceDetector;
import com.finance.domain.recurring.RecurringCandidate;
import com.finance.domain.recurring.RecurringExpense;
import com.finance.domain.recurring.RecurringExpenseRepository;
import com.finance.domain.transaction.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecurringExpenseServiceImpl implements RecurringExpenseService {

    // Long enough to see a yearly payment twice.
    private static final int HISTORY_YEARS = 3;

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final TransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;
    private final CategoryRepository categoryRepository;
    private final Clock clock;

    public RecurringExpenseServiceImpl(
            RecurringExpenseRepository recurringExpenseRepository, TransactionRepository transactionRepository,
            MerchantRepository merchantRepository, CategoryRepository categoryRepository, Clock clock) {
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.transactionRepository = transactionRepository;
        this.merchantRepository = merchantRepository;
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<RecurringExpense> detect(UUID userId) {
        recurringExpenseRepository.lockDetectionFor(userId);
        LocalDate today = LocalDate.now(clock);
        List<RecurringCandidate> candidates = RecurrenceDetector.detect(
                transactionRepository.findConfirmedExpensesSince(userId, today.minusYears(HISTORY_YEARS)), today);
        Map<String, RecurringExpense> stored = recurringExpenseRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(RecurringExpense::getGroupKey, Function.identity()));

        List<RecurringExpense> changed = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (RecurringCandidate candidate : candidates) {
            seen.add(candidate.groupKey());
            RecurringExpense existing = stored.get(candidate.groupKey());
            if (existing == null) {
                changed.add(new RecurringExpense(userId, displayName(userId, candidate), candidate));
            } else {
                existing.refreshFrom(candidate);
                changed.add(existing);
            }
        }
        stored.values().stream()
                .filter(expense -> !seen.contains(expense.getGroupKey()) && expense.isActive())
                .forEach(expense -> {
                    expense.markNoLongerDetected();
                    changed.add(expense);
                });
        recurringExpenseRepository.saveAll(changed);
        return list(userId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringExpense> list(UUID userId, boolean activeOnly) {
        return recurringExpenseRepository.findAllByUserId(userId).stream()
                .filter(expense -> !expense.isDismissed())
                .filter(expense -> !activeOnly || expense.isActive())
                .sorted(Comparator.comparing(RecurringExpense::isActive).reversed()
                        .thenComparing(RecurringExpense::getNextExpectedDate))
                .toList();
    }

    @Override
    @Transactional
    public RecurringExpense update(UUID userId, UUID id, UpdateRecurringExpenseCommand command) {
        RecurringExpense expense = requireOwned(userId, id);
        if (command.categoryId() != null) {
            categoryRepository.findVisibleByIdForUser(command.categoryId(), userId).orElseThrow(() -> new NotFoundException("Category not found."));
        }
        expense.edit(command.name(), command.categoryId(), command.confirmed());
        return recurringExpenseRepository.save(expense);
    }

    @Override
    @Transactional
    public void dismiss(UUID userId, UUID id) {
        RecurringExpense expense = requireOwned(userId, id);
        expense.dismiss(Instant.now(clock));
        recurringExpenseRepository.save(expense);
    }

    private RecurringExpense requireOwned(UUID userId, UUID id) {
        return recurringExpenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Recurring expense not found."));
    }

    private String displayName(UUID userId, RecurringCandidate candidate) {
        if (candidate.merchantId() != null) {
            return merchantRepository.findByIdAndUserId(candidate.merchantId(), userId)
                    .map(merchant -> merchant.getCanonicalName())
                    .orElse(candidate.description());
        }
        return candidate.description();
    }
}
