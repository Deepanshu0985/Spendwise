package com.finance.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByTokenHash(String tokenHash);

    List<Session> findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(UUID userId);

    void deleteByExpiresAtBeforeOrRevokedAtIsNotNull(Instant cutoff);
}
