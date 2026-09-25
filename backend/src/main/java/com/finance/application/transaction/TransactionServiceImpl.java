package com.finance.application.transaction;

import com.finance.application.exception.ApiError;
import com.finance.application.exception.DomainValidationException;
import com.finance.application.exception.NotFoundException;
import com.finance.domain.account.Account;
import com.finance.domain.account.AccountRepository;
import com.finance.domain.category.CategoryRepository;
import com.finance.domain.merchant.MerchantRepository;
import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionFilter;
import com.finance.domain.transaction.TransactionRepository;
import com.finance.domain.transaction.TransactionSource;
import com.finance.domain.transaction.TransactionSplit;
import com.finance.domain.transaction.TransactionSplitRepository;
import com.finance.domain.transaction.TransactionStatus;
import com.finance.domain.transaction.TransactionType;
import com.finance.domain.transaction.TransactionWithSplits;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {

    // TRANSFER_OUT/IN and CARD_PAYMENT_OUT/IN only ever come from
    // TransferService, as an atomic pair - never through plain create/update.
    // UNKNOWN is reserved for unresolved statement rows (Phase 6).
    private static final Set<TransactionType> MANUALLY_ASSIGNABLE_TYPES = EnumSet.of(
            TransactionType.EXPENSE,
            TransactionType.INCOME,
            TransactionType.REFUND,
            TransactionType.FEE_CHARGED,
            TransactionType.INTEREST_CHARGED,
            TransactionType.INTEREST_EARNED,
            TransactionType.CASH_WITHDRAWAL);

    private final TransactionRepository transactionRepository;
    private final TransactionSplitRepository splitRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final MerchantRepository merchantRepository;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            TransactionSplitRepository splitRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            MerchantRepository merchantRepository) {
        this.transactionRepository = transactionRepository;
        this.splitRepository = splitRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.merchantRepository = merchantRepository;
    }

    @Override
    @Transactional
    public TransactionWithSplits create(UUID userId, CreateTransactionCommand command) {
        requireManuallyAssignableType(command.transactionType());
        Account account = requireOwnedAccount(userId, command.accountId());
        requireMatchingCurrency(command.currency(), account.getCurrency());
        if (command.categoryId() != null) {
            requireVisibleCategory(userId, command.categoryId());
        }
        if (command.merchantId() != null) {
            requireOwnedMerchant(userId, command.merchantId());
        }

        Transaction transaction = new Transaction(
                userId,
                command.accountId(),
                command.merchantId(),
                command.categoryId(),
                command.transactionDate(),
                command.amount(),
                command.currency().toUpperCase(),
                command.description(),
                command.transactionType(),
                TransactionSource.MANUAL,
                TransactionStatus.CONFIRMED);
        transaction = transactionRepository.save(transaction);

        List<TransactionSplit> splits = saveSplits(userId, transaction, command.amount(), command.splits());
        return new TransactionWithSplits(transaction, splits);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionWithSplits> list(UUID userId, TransactionFilter filter, Pageable pageable) {
        Page<Transaction> page = transactionRepository.search(userId, filter, pageable);
        return page.map(transaction -> new TransactionWithSplits(transaction, splitRepository.findByTransactionId(transaction.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionWithSplits getOwned(UUID userId, UUID transactionId) {
        Transaction transaction = findOwned(userId, transactionId);
        return new TransactionWithSplits(transaction, splitRepository.findByTransactionId(transactionId));
    }

    @Override
    @Transactional
    public TransactionWithSplits update(UUID userId, UUID transactionId, UpdateTransactionCommand command) {
        Transaction transaction = findOwned(userId, transactionId);
        Account account = requireOwnedAccount(userId, command.accountId());
        requireMatchingCurrency(transaction.getCurrency(), account.getCurrency());
        if (command.categoryId() != null) {
            requireVisibleCategory(userId, command.categoryId());
        }
        if (command.merchantId() != null) {
            requireOwnedMerchant(userId, command.merchantId());
        }

        transaction.update(
                command.accountId(), command.merchantId(), command.categoryId(), command.transactionDate(), command.amount(), command.description());
        if (command.status() != null) {
            requireUpdatableStatus(command.status());
            transaction.setStatus(command.status());
        }
        transaction = transactionRepository.save(transaction);

        splitRepository.deleteByTransactionId(transactionId);
        List<TransactionSplit> splits = saveSplits(userId, transaction, command.amount(), command.splits());
        return new TransactionWithSplits(transaction, splits);
    }

    @Override
    @Transactional
    public void softDelete(UUID userId, UUID transactionId) {
        Transaction transaction = findOwned(userId, transactionId);
        transaction.setStatus(TransactionStatus.DELETED);
        transactionRepository.save(transaction);
    }

    private List<TransactionSplit> saveSplits(UUID userId, Transaction transaction, BigDecimal transactionAmount, List<SplitCommand> requestedSplits) {
        if (requestedSplits == null || requestedSplits.isEmpty()) {
            return List.of();
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (SplitCommand split : requestedSplits) {
            if (split.categoryId() != null) {
                requireVisibleCategory(userId, split.categoryId());
            }
            sum = sum.add(split.amount());
        }
        // Exact per ADR-013 - fixed-precision NUMERIC(19,4) end to end, never rounded (analytics-specification.md).
        if (sum.compareTo(transactionAmount) != 0) {
            throw new DomainValidationException(
                    "One or more fields are invalid.",
                    List.of(new ApiError.Detail("splits", "must sum to the transaction amount exactly")));
        }

        List<TransactionSplit> splits = requestedSplits.stream()
                .map(split -> new TransactionSplit(userId, transaction.getId(), split.categoryId(), split.amount()))
                .toList();
        return splitRepository.saveAll(splits);
    }

    private void requireManuallyAssignableType(TransactionType type) {
        if (!MANUALLY_ASSIGNABLE_TYPES.contains(type)) {
            throw new DomainValidationException(
                    "One or more fields are invalid.",
                    List.of(new ApiError.Detail(
                            "transactionType",
                            "must be one of " + MANUALLY_ASSIGNABLE_TYPES
                                    + " - transfer/card-payment types are only created via POST /transactions/transfer")));
        }
    }

    private void requireUpdatableStatus(TransactionStatus status) {
        if (status == TransactionStatus.DELETED) {
            throw new DomainValidationException(
                    "One or more fields are invalid.",
                    List.of(new ApiError.Detail("status", "use DELETE /transactions/{id} to delete a transaction")));
        }
    }

    private void requireMatchingCurrency(String transactionCurrency, String accountCurrency) {
        if (!transactionCurrency.equalsIgnoreCase(accountCurrency)) {
            throw new DomainValidationException(
                    "One or more fields are invalid.",
                    List.of(new ApiError.Detail("currency", "must match the account's currency - cross-currency conversion is out of scope for V1")));
        }
    }

    private Account requireOwnedAccount(UUID userId, UUID accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Account not found."));
    }

    private void requireVisibleCategory(UUID userId, UUID categoryId) {
        categoryRepository.findVisibleByIdForUser(categoryId, userId)
                .orElseThrow(() -> new NotFoundException("Category not found."));
    }

    private void requireOwnedMerchant(UUID userId, UUID merchantId) {
        merchantRepository.findByIdAndUserId(merchantId, userId)
                .orElseThrow(() -> new NotFoundException("Merchant not found."));
    }

    private Transaction findOwned(UUID userId, UUID transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new NotFoundException("Transaction not found."));
    }
}
