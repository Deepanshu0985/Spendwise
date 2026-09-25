package com.finance.application.account;

import com.finance.application.exception.NotFoundException;
import com.finance.domain.account.Account;
import com.finance.domain.account.AccountRepository;
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
    public Account create(UUID userId, CreateAccountCommand command) {
        Account account = new Account(
                userId,
                command.name(),
                command.accountType(),
                command.institutionName(),
                command.last4(),
                command.currency().toUpperCase());
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
    public Account update(UUID userId, UUID accountId, UpdateAccountCommand command) {
        Account account = findOwned(userId, accountId);
        account.update(command.name(), command.institutionName(), command.last4());
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
