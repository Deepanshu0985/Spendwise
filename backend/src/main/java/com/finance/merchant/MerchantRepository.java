package com.finance.merchant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    List<Merchant> findByUserIdOrderByCanonicalName(UUID userId);

    Optional<Merchant> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndNormalizedKey(UUID userId, String normalizedKey);
}
