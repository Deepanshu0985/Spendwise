package com.finance.infrastructure.persistence.account;

import com.finance.domain.account.Account;

/** Pure mapping logic, not swappable business behavior - no interface, same reasoning as CurrentUserGuard. */
final class AccountMapper {

    private AccountMapper() {
    }

    static Account toDomain(AccountJpaEntity entity) {
        return new Account(
                entity.getId(),
                entity.getUserId(),
                entity.getName(),
                entity.getAccountType(),
                entity.getInstitutionName(),
                entity.getLast4(),
                entity.getCurrency(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    static AccountJpaEntity toNewEntity(Account account) {
        return new AccountJpaEntity(
                account.getId(),
                account.getUserId(),
                account.getName(),
                account.getAccountType(),
                account.getInstitutionName(),
                account.getLast4(),
                account.getCurrency(),
                account.isActive());
    }

    /** Mutates an existing managed entity in place so Hibernate's dirty checking (and @UpdateTimestamp) fires correctly. */
    static AccountJpaEntity applyChanges(AccountJpaEntity entity, Account account) {
        entity.setName(account.getName());
        entity.setInstitutionName(account.getInstitutionName());
        entity.setLast4(account.getLast4());
        entity.setActive(account.isActive());
        return entity;
    }
}
