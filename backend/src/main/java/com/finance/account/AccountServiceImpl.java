package com.finance.account;

import com.finance.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public Account create(UUID userId, CreateAccountRequest request) {
        Account account = new Account(
                userId,
                request.name(),
                request.accountType(),
                request.institutionName(),
                request.last4(),
                request.currency().toUpperCase());
        return accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> listForUser(UUID userId) {
        return accountRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Account getOwned(UUID userId, UUID accountId) {
        return findOwned(userId, accountId);
    }

    @Override
    @Transactional
    public Account update(UUID userId, UUID accountId, UpdateAccountRequest request) {
        Account account = findOwned(userId, accountId);
        account.update(request.name(), request.institutionName(), request.last4());
        return accountRepository.save(account);
    }

    @Override
    @Transactional
    public void deactivate(UUID userId, UUID accountId) {
        Account account = findOwned(userId, accountId);
        account.deactivate();
        accountRepository.save(account);
    }

    private Account findOwned(UUID userId, UUID accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Account not found."));
    }
}
