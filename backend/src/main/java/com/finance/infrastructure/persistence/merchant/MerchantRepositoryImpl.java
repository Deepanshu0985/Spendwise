package com.finance.infrastructure.persistence.merchant;

import com.finance.domain.merchant.Merchant;
import com.finance.domain.merchant.MerchantRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MerchantRepositoryImpl implements MerchantRepository {

    private final MerchantJpaRepository jpaRepository;

    public MerchantRepositoryImpl(MerchantJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Merchant save(Merchant merchant) {
        MerchantJpaEntity entity = jpaRepository.findById(merchant.getId())
                .map(existing -> MerchantMapper.applyChanges(existing, merchant))
                .orElseGet(() -> MerchantMapper.toNewEntity(merchant));
        return MerchantMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<Merchant> findByUserIdOrderByCanonicalName(UUID userId) {
        return jpaRepository.findByUserIdOrderByCanonicalName(userId).stream().map(MerchantMapper::toDomain).toList();
    }

    @Override
    public Optional<Merchant> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(MerchantMapper::toDomain);
    }

    @Override
    public boolean existsByUserIdAndNormalizedKey(UUID userId, String normalizedKey) {
        return jpaRepository.existsByUserIdAndNormalizedKey(userId, normalizedKey);
    }
}
