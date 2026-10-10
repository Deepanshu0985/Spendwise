package com.finance.infrastructure.persistence.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BudgetCategoryJpaRepository extends JpaRepository<BudgetCategoryJpaEntity, UUID> {

    @Modifying
    @Query("delete from BudgetCategoryJpaEntity c where c.budgetId = :budgetId")
    void deleteByBudgetId(@Param("budgetId") UUID budgetId);

    List<BudgetCategoryJpaEntity> findByBudgetIdIn(Collection<UUID> budgetIds);
}
