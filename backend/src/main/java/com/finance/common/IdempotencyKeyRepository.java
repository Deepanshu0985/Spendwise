package com.finance.common;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyRecord, UUID> {

    Optional<IdempotencyKeyRecord> findByUserIdAndKeyAndEndpoint(UUID userId, String key, String endpoint);
}
