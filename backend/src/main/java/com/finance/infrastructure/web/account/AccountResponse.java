package com.finance.infrastructure.web.account;

import com.finance.domain.account.Account;
import com.finance.domain.account.AccountType;

import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        AccountType accountType,
        String institutionName,
        String last4,
        String currency,
        boolean active) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getAccountType(),
                account.getInstitutionName(),
                account.getLast4(),
                account.getCurrency(),
                account.isActive());
    }
}
