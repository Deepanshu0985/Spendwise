package com.finance.domain.merchant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port - implemented by infrastructure.persistence.merchant.MerchantRepositoryImpl. No Spring Data / JPA types here. */
public interface MerchantRepository {

    Merchant save(Merchant merchant);

    List<Merchant> findByUserIdOrderByCanonicalName(UUID userId);

    Optional<Merchant> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndNormalizedKey(UUID userId, String normalizedKey);
}
