package com.finance.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Explicit user_id filtering here is intentional defense-in-depth alongside
    // RLS (api-specification.md's Authorization section), not redundant with it.
    Optional<Account> findByIdAndUserId(UUID id, UUID userId);
}
