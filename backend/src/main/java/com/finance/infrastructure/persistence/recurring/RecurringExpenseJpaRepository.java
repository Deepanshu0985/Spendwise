package com.finance.infrastructure.persistence.recurring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringExpenseJpaRepository extends JpaRepository<RecurringExpenseJpaEntity, UUID> {

    // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
    Optional<RecurringExpenseJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    List<RecurringExpenseJpaEntity> findByUserId(UUID userId);

    // A transaction-scoped advisory lock keyed on the user; released automatically at commit or rollback.
    @Query(value = "select 1 from (select pg_advisory_xact_lock(hashtext(cast(:userId as text)))) locked", nativeQuery = true)
    int lockDetectionFor(@Param("userId") UUID userId);
}
