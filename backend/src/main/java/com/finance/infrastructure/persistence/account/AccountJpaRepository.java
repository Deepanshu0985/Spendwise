package com.finance.infrastructure.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, UUID> {

    List<AccountJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<AccountJpaEntity> findByIdAndUserId(UUID id, UUID userId);
}
