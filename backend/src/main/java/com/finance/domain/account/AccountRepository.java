package com.finance.domain.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port - implemented by infrastructure.persistence.account.AccountRepositoryImpl. No Spring Data / JPA types here. */
public interface AccountRepository {

    Account save(Account account);

    List<Account> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Explicit user_id filtering here is intentional defense-in-depth alongside
    // RLS (api-specification.md's Authorization section), not redundant with it.
    Optional<Account> findByIdAndUserId(UUID id, UUID userId);
}
