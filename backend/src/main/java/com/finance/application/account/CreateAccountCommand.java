package com.finance.application.account;

import com.finance.domain.account.AccountType;

/** Application-layer input, distinct from the HTTP request DTO - mapped by infrastructure.web.account.AccountController. */
public record CreateAccountCommand(String name, AccountType accountType, String institutionName, String last4, String currency) {
}
