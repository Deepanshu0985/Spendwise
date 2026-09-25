package com.finance.infrastructure.persistence.merchant;

import com.finance.domain.merchant.Merchant;

/** Pure mapping logic, not swappable business behavior - no interface, same reasoning as CurrentUserGuard. */
final class MerchantMapper {

    private MerchantMapper() {
    }

    static Merchant toDomain(MerchantJpaEntity entity) {
        return new Merchant(
                entity.getId(), entity.getUserId(), entity.getCanonicalName(), entity.getNormalizedKey(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    static MerchantJpaEntity toNewEntity(Merchant merchant) {
        return new MerchantJpaEntity(merchant.getId(), merchant.getUserId(), merchant.getCanonicalName(), merchant.getNormalizedKey());
    }

    /** Mutates an existing managed entity in place so Hibernate's dirty checking (and @UpdateTimestamp) fires correctly. */
    static MerchantJpaEntity applyChanges(MerchantJpaEntity entity, Merchant merchant) {
        entity.setCanonicalName(merchant.getCanonicalName());
        entity.setNormalizedKey(merchant.getNormalizedKey());
        return entity;
    }
}
