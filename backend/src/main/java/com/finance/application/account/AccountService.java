package com.finance.application.account;

import com.finance.domain.account.Account;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    Account create(UUID userId, CreateAccountCommand command);

    List<Account> listForUser(UUID userId);

    Account getOwned(UUID userId, UUID accountId);

    Account update(UUID userId, UUID accountId, UpdateAccountCommand command);

    void deactivate(UUID userId, UUID accountId);
}
