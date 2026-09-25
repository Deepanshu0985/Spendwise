package com.finance.infrastructure.persistence.merchant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantJpaRepository extends JpaRepository<MerchantJpaEntity, UUID> {

    List<MerchantJpaEntity> findByUserIdOrderByCanonicalName(UUID userId);

    Optional<MerchantJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndNormalizedKey(UUID userId, String normalizedKey);
}
