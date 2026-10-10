package com.finance.infrastructure.persistence.budget;

import com.finance.domain.budget.Budget;
import com.finance.domain.budget.BudgetCategoryLimit;
import com.finance.domain.budget.BudgetRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class BudgetRepositoryImpl implements BudgetRepository {

    private final BudgetJpaRepository jpaRepository;
    private final BudgetCategoryJpaRepository categoryJpaRepository;

    public BudgetRepositoryImpl(BudgetJpaRepository jpaRepository, BudgetCategoryJpaRepository categoryJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.categoryJpaRepository = categoryJpaRepository;
    }

    @Override
    public Budget save(Budget budget) {
        BudgetJpaEntity entity = jpaRepository.findById(budget.getId())
                .orElseGet(() -> new BudgetJpaEntity(budget.getId(), budget.getUserId(), budget.getCurrency()));
        entity.setName(budget.getName());
        entity.setPeriodType(budget.getPeriodType());
        entity.setStartDate(budget.getStartDate());
        entity.setEndDate(budget.getEndDate());
        entity.setTotalLimit(budget.getTotalLimit());
        entity.setActive(budget.isActive());
        jpaRepository.saveAndFlush(entity);

        // The category limits are replaced as a set; the unique (budget, category) key is only checked once the old rows are gone.
        categoryJpaRepository.deleteByBudgetId(budget.getId());
        categoryJpaRepository.flush();
        categoryJpaRepository.saveAll(budget.getCategoryLimits().stream()
                .map(limit -> new BudgetCategoryJpaEntity(budget.getUserId(), budget.getId(), limit.categoryId(), limit.limitAmount()))
                .toList());
        return budget;
    }

    @Override
    public Optional<Budget> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId)
                .map(entity -> toDomain(entity, categoryJpaRepository.findByBudgetIdIn(List.of(entity.getId()))));
    }

    @Override
    public List<Budget> findByUserId(UUID userId) {
        List<BudgetJpaEntity> entities = jpaRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (entities.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<BudgetCategoryJpaEntity>> byBudget = categoryJpaRepository
                .findByBudgetIdIn(entities.stream().map(BudgetJpaEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(BudgetCategoryJpaEntity::getBudgetId));
        return entities.stream().map(entity -> toDomain(entity, byBudget.getOrDefault(entity.getId(), List.of()))).toList();
    }

    @Override
    public void delete(Budget budget) {
        categoryJpaRepository.deleteByBudgetId(budget.getId());
        categoryJpaRepository.flush();
        jpaRepository.deleteById(budget.getId());
    }

    private static Budget toDomain(BudgetJpaEntity e, List<BudgetCategoryJpaEntity> categories) {
        return new Budget(
                e.getId(), e.getUserId(), e.getName(), e.getPeriodType(), e.getStartDate(), e.getEndDate(), e.getTotalLimit(),
                e.getCurrency(), e.isActive(),
                categories.stream().map(c -> new BudgetCategoryLimit(c.getCategoryId(), c.getLimitAmount())).toList());
    }
}
