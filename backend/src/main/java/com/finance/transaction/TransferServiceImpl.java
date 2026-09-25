package com.finance.transaction;

import com.finance.account.Account;
import com.finance.account.AccountRepository;
import com.finance.account.AccountType;
import com.finance.common.error.ApiError;
import com.finance.common.exception.DomainValidationException;
import com.finance.common.exception.NotFoundException;
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
    public TransferResult create(UUID userId, CreateTransferRequest request) {
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new DomainValidationException(
                    "One or more fields are invalid.",
                    List.of(new ApiError.Detail("toAccountId", "must differ from fromAccountId")));
        }

        Account fromAccount = requireOwnedAccount(userId, request.fromAccountId());
        Account toAccount = requireOwnedAccount(userId, request.toAccountId());
        requireMatchingCurrency(request.currency(), fromAccount.getCurrency(), toAccount.getCurrency());

        if (request.kind() == TransferKind.CARD_PAYMENT && toAccount.getAccountType() != AccountType.CREDIT_CARD) {
            throw new DomainValidationException(
                    "One or more fields are invalid.",
                    List.of(new ApiError.Detail("toAccountId", "a card payment must credit a CREDIT_CARD account")));
        }

        TransactionType outType = request.kind() == TransferKind.CARD_PAYMENT
                ? TransactionType.CARD_PAYMENT_OUT
                : TransactionType.TRANSFER_OUT;
        TransactionType inType = request.kind() == TransferKind.CARD_PAYMENT
                ? TransactionType.CARD_PAYMENT_IN
                : TransactionType.TRANSFER_IN;

        UUID transferGroupId = UUID.randomUUID();
        String currency = request.currency().toUpperCase();
        String description = (request.kind() == TransferKind.CARD_PAYMENT ? "Card payment" : "Transfer") + " to " + toAccount.getName();
        String inDescription = (request.kind() == TransferKind.CARD_PAYMENT ? "Card payment" : "Transfer") + " from " + fromAccount.getName();

        Transaction outTransaction = new Transaction(
                userId, request.fromAccountId(), null, null, request.transactionDate(), request.amount(), currency,
                description, outType, TransactionSource.MANUAL, TransactionStatus.CONFIRMED);
        outTransaction.setTransferGroupId(transferGroupId);

        Transaction inTransaction = new Transaction(
                userId, request.toAccountId(), null, null, request.transactionDate(), request.amount(), currency,
                inDescription, inType, TransactionSource.MANUAL, TransactionStatus.CONFIRMED);
        inTransaction.setTransferGroupId(transferGroupId);

        transactionRepository.save(outTransaction);
        transactionRepository.save(inTransaction);

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
