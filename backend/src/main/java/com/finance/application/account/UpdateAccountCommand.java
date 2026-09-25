package com.finance.application.account;

/** accountType and currency are immutable after creation - changing either would
 * put existing transaction history and analytics in an inconsistent state. */
public record UpdateAccountCommand(String name, String institutionName, String last4) {
}
