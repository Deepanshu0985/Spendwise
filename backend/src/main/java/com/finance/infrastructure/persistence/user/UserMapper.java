package com.finance.infrastructure.persistence.user;

import com.finance.domain.user.User;

/** Pure mapping logic, not swappable business behavior - no interface, same reasoning as CurrentUserGuard. */
final class UserMapper {

    private UserMapper() {
    }

    static User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getFullName(),
                entity.getDefaultCurrency(),
                entity.getTimezone(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt());
    }

    static UserJpaEntity toNewEntity(User user) {
        return new UserJpaEntity(
                user.getId(), user.getEmail(), user.getPasswordHash(), user.getFullName(), user.getDefaultCurrency(), user.getTimezone());
    }

    /** Mutates an existing managed entity in place so Hibernate's dirty checking (and @UpdateTimestamp) fires correctly. */
    static UserJpaEntity applyChanges(UserJpaEntity entity, User user) {
        entity.setPasswordHash(user.getPasswordHash());
        return entity;
    }
}
