package com.finance.application.bulk;

import com.finance.application.exception.ApiError;
import com.finance.application.exception.DomainValidationException;
import com.finance.application.exception.NotFoundException;
import com.finance.application.statement.PayeeRuleService;
import com.finance.application.statement.PayeeRuleService.RuleChange;
import com.finance.domain.category.CategoryRepository;
import com.finance.domain.merchant.MerchantRepository;
import com.finance.domain.statement.Statement;
import com.finance.domain.statement.StatementRepository;
import com.finance.domain.statement.StatementStatus;
import com.finance.domain.statement.StatementTransaction;
import com.finance.domain.statement.StatementTransactionRepository;
import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionRepository;
import com.finance.domain.transaction.TransactionSplitRepository;
import com.finance.domain.transaction.TransactionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class BulkCategorizationServiceImpl implements BulkCategorizationService {

    private final StatementRepository statementRepository;
    private final StatementTransactionRepository stagedRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionSplitRepository splitRepository;
    private final CategoryRepository categoryRepository;
    private final MerchantRepository merchantRepository;
    private final PayeeRuleService payeeRuleService;

    public BulkCategorizationServiceImpl(
            StatementRepository statementRepository,
            StatementTransactionRepository stagedRepository,
            TransactionRepository transactionRepository,
            TransactionSplitRepository splitRepository,
            CategoryRepository categoryRepository,
            MerchantRepository merchantRepository,
            PayeeRuleService payeeRuleService) {
        this.statementRepository = statementRepository;
        this.stagedRepository = stagedRepository;
        this.transactionRepository = transactionRepository;
        this.splitRepository = splitRepository;
        this.categoryRepository = categoryRepository;
        this.merchantRepository = merchantRepository;
        this.payeeRuleService = payeeRuleService;
    }

    // ----- rows of a statement awaiting review -----

    @Override
    @Transactional
    public StagedResult changeStagedRows(UUID userId, UUID statementId, StagedChange change) {
        requireAwaitingReview(userId, statementId);
        Set<UUID> ids = distinctIds(change.rowIds(), "rowIds");
        requireSomethingToSet(change.categoryId(), change.merchantId());
        requireVisibleCategory(userId, change.categoryId());
        requireOwnedMerchant(userId, change.merchantId());

        List<StatementTransaction> all = stagedRepository.findByStatementIdAndUserId(statementId, userId);
        Map<UUID, StatementTransaction> byId = new HashMap<>();
        all.forEach(row -> byId.put(row.getId(), row));
        // Every requested row must be one of this person's rows of this statement, or nothing at all is changed.
        List<StatementTransaction> selected = new ArrayList<>();
        for (UUID id : ids) {
            StatementTransaction row = byId.get(id);
            if (row == null) {
                throw new NotFoundException("One or more of those rows were not found in this statement.");
            }
            selected.add(row);
        }

        Map<UUID, RowSnapshot> before = new HashMap<>();
        all.forEach(row -> before.put(row.getId(), snapshot(row)));

        selected.forEach(row -> row.applyBulkChange(change.merchantId(), change.categoryId()));
        stagedRepository.saveAll(selected);

        RuleChange rules = RuleChange.none();
        if (change.rememberPayee()) {
            rules = payeeRuleService.rememberAll(userId, selected);
            Set<UUID> selectedIds = ids;
            List<StatementTransaction> others = all.stream().filter(row -> !selectedIds.contains(row.getId())).toList();
            stagedRepository.saveAll(payeeRuleService.applyTo(userId, others));
        }

        List<StatementTransaction> after = stagedRepository.findByStatementIdAndUserId(statementId, userId);
        List<RowSnapshot> undoRows = after.stream()
                .filter(row -> !snapshot(row).equals(before.get(row.getId())))
                .map(row -> before.get(row.getId()))
                .toList();
        return new StagedResult(after, selected.size(), new StagedUndo(undoRows, rules));
    }

    @Override
    @Transactional
    public List<StatementTransaction> undoStagedRows(UUID userId, UUID statementId, StagedUndo undo) {
        requireAwaitingReview(userId, statementId);
        if (undo.rows() == null || undo.rows().size() > MAX_ROWS * 2) {
            throw invalid("rows", "must list the rows to put back");
        }
        Map<UUID, StatementTransaction> byId = new HashMap<>();
        stagedRepository.findByStatementIdAndUserId(statementId, userId).forEach(row -> byId.put(row.getId(), row));
        for (RowSnapshot snapshot : undo.rows()) {
            if (!byId.containsKey(snapshot.id())) {
                throw new NotFoundException("One or more of those rows were not found in this statement.");
            }
            requireVisibleCategory(userId, snapshot.categoryId());
            requireOwnedMerchant(userId, snapshot.merchantId());
        }
        for (RowSnapshot snapshot : undo.rows()) {
            byId.get(snapshot.id()).restoreReviewState(
                    snapshot.merchantId(), snapshot.categoryId(), snapshot.reviewStatus(), snapshot.aiSuggested(), snapshot.aiReason());
        }
        stagedRepository.saveAll(new ArrayList<>(byId.values()));
        if (undo.rules() != null) {
            payeeRuleService.undo(userId, undo.rules());
        }
        return stagedRepository.findByStatementIdAndUserId(statementId, userId);
    }

    // ----- saved transactions -----

    @Override
    @Transactional
    public LedgerResult changeTransactions(UUID userId, LedgerChange change) {
        Set<UUID> ids = distinctIds(change.transactionIds(), "transactionIds");
        requireSomethingToSet(change.categoryId(), change.merchantId());
        requireVisibleCategory(userId, change.categoryId());
        requireOwnedMerchant(userId, change.merchantId());

        List<Transaction> found = new ArrayList<>();
        for (UUID id : ids) {
            Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                    .filter(t -> t.getStatus() != TransactionStatus.DELETED)
                    .orElseThrow(() -> new NotFoundException("One or more of those transactions were not found."));
            found.add(transaction);
        }

        List<Transaction> changed = new ArrayList<>();
        List<TransactionSnapshot> undo = new ArrayList<>();
        int skipped = 0;
        for (Transaction transaction : found) {
            if (!splitRepository.findByTransactionId(transaction.getId()).isEmpty()) {
                skipped++;
                continue;
            }
            undo.add(new TransactionSnapshot(transaction.getId(), transaction.getMerchantId(), transaction.getCategoryId()));
            setCategoryAndMerchant(transaction, change.merchantId() != null ? change.merchantId() : transaction.getMerchantId(),
                    change.categoryId() != null ? change.categoryId() : transaction.getCategoryId());
            changed.add(transactionRepository.save(transaction));
        }
        return new LedgerResult(changed, skipped, undo);
    }

    @Override
    @Transactional
    public List<Transaction> undoTransactions(UUID userId, List<TransactionSnapshot> snapshots) {
        if (snapshots == null || snapshots.size() > MAX_ROWS) {
            throw invalid("rows", "must list the transactions to put back");
        }
        List<Transaction> restored = new ArrayList<>();
        for (TransactionSnapshot snapshot : snapshots) {
            Transaction transaction = transactionRepository.findByIdAndUserId(snapshot.id(), userId)
                    .filter(t -> t.getStatus() != TransactionStatus.DELETED)
                    .orElseThrow(() -> new NotFoundException("One or more of those transactions were not found."));
            requireVisibleCategory(userId, snapshot.categoryId());
            requireOwnedMerchant(userId, snapshot.merchantId());
            setCategoryAndMerchant(transaction, snapshot.merchantId(), snapshot.categoryId());
            restored.add(transactionRepository.save(transaction));
        }
        return restored;
    }

    // ----- helpers -----

    private static void setCategoryAndMerchant(Transaction transaction, UUID merchantId, UUID categoryId) {
        transaction.update(
                transaction.getAccountId(), merchantId, categoryId, transaction.getTransactionDate(), transaction.getAmount(),
                transaction.getDescription());
    }

    private static RowSnapshot snapshot(StatementTransaction row) {
        return new RowSnapshot(
                row.getId(), row.getSuggestedMerchantId(), row.getSuggestedCategoryId(), row.getReviewStatus(), row.isAiSuggested(),
                row.getAiReason());
    }

    private void requireAwaitingReview(UUID userId, UUID statementId) {
        Statement statement = statementRepository.findByIdAndUserId(statementId, userId)
                .orElseThrow(() -> new NotFoundException("Statement not found."));
        if (statement.getStatus() != StatementStatus.READY_FOR_REVIEW) {
            throw new DomainValidationException(
                    "Rows can only be changed while the statement is awaiting review (current status: " + statement.getStatus() + ").",
                    List.of());
        }
    }

    private static Set<UUID> distinctIds(List<UUID> ids, String field) {
        if (ids == null || ids.isEmpty()) {
            throw invalid(field, "must list at least one");
        }
        Set<UUID> distinct = new LinkedHashSet<>(ids);
        if (distinct.size() > MAX_ROWS || distinct.stream().anyMatch(Objects::isNull)) {
            throw invalid(field, "must list between 1 and " + MAX_ROWS + " ids");
        }
        return distinct;
    }

    private static void requireSomethingToSet(UUID categoryId, UUID merchantId) {
        if (categoryId == null && merchantId == null) {
            throw invalid("categoryId", "choose a category or a merchant to set");
        }
    }

    private void requireVisibleCategory(UUID userId, UUID categoryId) {
        if (categoryId != null) {
            categoryRepository.findVisibleByIdForUser(categoryId, userId).orElseThrow(() -> new NotFoundException("Category not found."));
        }
    }

    private void requireOwnedMerchant(UUID userId, UUID merchantId) {
        if (merchantId != null) {
            merchantRepository.findByIdAndUserId(merchantId, userId).orElseThrow(() -> new NotFoundException("Merchant not found."));
        }
    }

    private static DomainValidationException invalid(String field, String reason) {
        return new DomainValidationException("One or more fields are invalid.", List.of(new ApiError.Detail(field, reason)));
    }
}
