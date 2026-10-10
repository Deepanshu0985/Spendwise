package com.finance.infrastructure.persistence.recurring;

import com.finance.domain.recurring.RecurringExpense;
import com.finance.domain.recurring.RecurringExpenseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RecurringExpenseRepositoryImpl implements RecurringExpenseRepository {

    private final RecurringExpenseJpaRepository jpaRepository;

    public RecurringExpenseRepositoryImpl(RecurringExpenseJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public RecurringExpense save(RecurringExpense recurringExpense) {
        RecurringExpenseJpaEntity entity = jpaRepository.findById(recurringExpense.getId())
                .orElseGet(() -> new RecurringExpenseJpaEntity(
                        recurringExpense.getId(), recurringExpense.getUserId(), recurringExpense.getGroupKey(), recurringExpense.getCurrency()));
        copyInto(entity, recurringExpense);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<RecurringExpense> saveAll(List<RecurringExpense> recurringExpenses) {
        return recurringExpenses.stream().map(this::save).toList();
    }

    @Override
    public Optional<RecurringExpense> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(RecurringExpenseRepositoryImpl::toDomain);
    }

    @Override
    public List<RecurringExpense> findAllByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream().map(RecurringExpenseRepositoryImpl::toDomain).toList();
    }

    @Override
    public void lockDetectionFor(UUID userId) {
        jpaRepository.lockDetectionFor(userId);
    }

    private static void copyInto(RecurringExpenseJpaEntity entity, RecurringExpense domain) {
        entity.setMerchantId(domain.getMerchantId());
        entity.setCategoryId(domain.getCategoryId());
        entity.setName(domain.getName());
        entity.setAverageAmount(domain.getAverageAmount());
        entity.setFrequency(domain.getFrequency());
        entity.setLastSeenDate(domain.getLastSeenDate());
        entity.setNextExpectedDate(domain.getNextExpectedDate());
        entity.setMonthlyEstimate(domain.getMonthlyEstimate());
        entity.setYearlyEstimate(domain.getYearlyEstimate());
        entity.setOccurrences(domain.getOccurrences());
        entity.setConfidenceScore(domain.getConfidenceScore());
        entity.setActive(domain.isActive());
        entity.setConfirmed(domain.isConfirmed());
        entity.setDismissedAt(domain.getDismissedAt());
    }

    private static RecurringExpense toDomain(RecurringExpenseJpaEntity e) {
        return new RecurringExpense(
                e.getId(), e.getUserId(), e.getGroupKey(), e.getMerchantId(), e.getCategoryId(), e.getName(), e.getCurrency(),
                e.getAverageAmount(), e.getFrequency(), e.getLastSeenDate(), e.getNextExpectedDate(), e.getMonthlyEstimate(),
                e.getOccurrences(), e.getConfidenceScore(), e.isActive(), e.isConfirmed(), e.getDismissedAt());
    }
}
