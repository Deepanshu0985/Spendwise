package com.finance.application.transaction;

import com.finance.application.exception.ApiError;
import com.finance.application.exception.DomainValidationException;
import com.finance.application.exception.NotFoundException;
import com.finance.domain.account.Account;
import com.finance.domain.account.AccountRepository;
import com.finance.domain.account.AccountType;
import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionRepository;
import com.finance.domain.transaction.TransactionSource;
import com.finance.domain.transaction.TransactionStatus;
import com.finance.domain.transaction.TransactionType;
import com.finance.domain.transaction.TransferKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manual creation always requires both accounts to be valid, owned accounts -
 * unlike statement import (Phase 6), where a transfer's counterpart may be an
 * untracked external account and the row is created one-sided and flagged for
 * review (analytics-specification.md). That one-sided case does not apply
 * here: this endpoint's request DTO requires both account ids.
 */
@Service
public class TransferServiceImpl implements TransferService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransferServiceImpl(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public TransferResult create(UUID userId, CreateTransferCommand command) {
        if (command.fromAccountId().equals(command.toAccountId())) {
            throw new DomainValidationException(
                    "One or more fields are invalid.",
                    List.of(new ApiError.Detail("toAccountId", "must differ from fromAccountId")));
        }

        Account fromAccount = requireOwnedAccount(userId, command.fromAccountId());
        Account toAccount = requireOwnedAccount(userId, command.toAccountId());
        requireMatchingCurrency(command.currency(), fromAccount.getCurrency(), toAccount.getCurrency());

        if (command.kind() == TransferKind.CARD_PAYMENT && toAccount.getAccountType() != AccountType.CREDIT_CARD) {
            throw new DomainValidationException(
                    "One or more fields are invalid.",
                    List.of(new ApiError.Detail("toAccountId", "a card payment must credit a CREDIT_CARD account")));
        }

        TransactionType outType = command.kind() == TransferKind.CARD_PAYMENT
                ? TransactionType.CARD_PAYMENT_OUT
                : TransactionType.TRANSFER_OUT;
        TransactionType inType = command.kind() == TransferKind.CARD_PAYMENT
                ? TransactionType.CARD_PAYMENT_IN
                : TransactionType.TRANSFER_IN;

        UUID transferGroupId = UUID.randomUUID();
        String currency = command.currency().toUpperCase();
        String description = (command.kind() == TransferKind.CARD_PAYMENT ? "Card payment" : "Transfer") + " to " + toAccount.getName();
        String inDescription = (command.kind() == TransferKind.CARD_PAYMENT ? "Card payment" : "Transfer") + " from " + fromAccount.getName();

        Transaction outTransaction = new Transaction(
                userId, command.fromAccountId(), null, null, command.transactionDate(), command.amount(), currency,
                description, outType, TransactionSource.MANUAL, TransactionStatus.CONFIRMED);
        outTransaction.setTransferGroupId(transferGroupId);

        Transaction inTransaction = new Transaction(
                userId, command.toAccountId(), null, null, command.transactionDate(), command.amount(), currency,
                inDescription, inType, TransactionSource.MANUAL, TransactionStatus.CONFIRMED);
        inTransaction.setTransferGroupId(transferGroupId);

        outTransaction = transactionRepository.save(outTransaction);
        inTransaction = transactionRepository.save(inTransaction);

        return new TransferResult(outTransaction, inTransaction);
    }

    private void requireMatchingCurrency(String requestCurrency, String fromCurrency, String toCurrency) {
        if (!requestCurrency.equalsIgnoreCase(fromCurrency) || !requestCurrency.equalsIgnoreCase(toCurrency)) {
            throw new DomainValidationException(
                    "One or more fields are invalid.",
                    List.of(new ApiError.Detail("currency", "must match both accounts' currency - cross-currency transfers are out of scope for V1")));
        }
    }

    private Account requireOwnedAccount(UUID userId, UUID accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Account not found."));
    }
}
