package com.finance.infrastructure.persistence.account;

import com.finance.domain.account.Account;
import com.finance.domain.account.AccountRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AccountRepositoryImpl implements AccountRepository {

    private final AccountJpaRepository jpaRepository;

    public AccountRepositoryImpl(AccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = jpaRepository.findById(account.getId())
                .map(existing -> AccountMapper.applyChanges(existing, account))
                .orElseGet(() -> AccountMapper.toNewEntity(account));
        return AccountMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<Account> findByUserIdOrderByCreatedAtDesc(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(AccountMapper::toDomain).toList();
    }

    @Override
    public Optional<Account> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(AccountMapper::toDomain);
    }
}
