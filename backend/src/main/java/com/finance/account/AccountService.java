package com.finance.account;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    Account create(UUID userId, CreateAccountRequest request);

    List<Account> listForUser(UUID userId);

    Account getOwned(UUID userId, UUID accountId);

    Account update(UUID userId, UUID accountId, UpdateAccountRequest request);

    void deactivate(UUID userId, UUID accountId);
}
