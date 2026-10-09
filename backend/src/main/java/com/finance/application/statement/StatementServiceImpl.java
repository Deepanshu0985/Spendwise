package com.finance.application.statement;

import com.finance.application.exception.ApiException;
import com.finance.application.exception.DomainValidationException;
import com.finance.application.exception.NotFoundException;
import com.finance.application.exception.StatementProcessingFailedException;
import com.finance.domain.account.Account;
import com.finance.domain.account.AccountRepository;
import com.finance.domain.statement.ParsedStatement;
import com.finance.domain.statement.ParsedTransactionRow;
import com.finance.domain.statement.ReviewStatus;
import com.finance.domain.statement.Statement;
import com.finance.domain.statement.StatementRepository;
import com.finance.domain.statement.StatementStatus;
import com.finance.domain.statement.StatementTransaction;
import com.finance.domain.statement.StatementTransactionRepository;
import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionRepository;
import com.finance.domain.transaction.TransactionSource;
import com.finance.domain.transaction.TransactionStatus;
import com.finance.infrastructure.pdf.ExtractionResult;
import com.finance.infrastructure.pdf.NormalizedTransactionRow;
import com.finance.infrastructure.pdf.PdfTextExtractor;
import com.finance.infrastructure.pdf.PdfValidator;
import com.finance.infrastructure.pdf.StatementFormatDetector;
import com.finance.infrastructure.pdf.StatementParser;
import com.finance.infrastructure.pdf.DescriptionCleanerRegistry;
import com.finance.infrastructure.pdf.TransactionNormalizer;
import com.finance.infrastructure.storage.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StatementServiceImpl implements StatementService {

    private final StatementRepository statementRepository;
    private final StatementTransactionRepository statementTransactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PdfValidator pdfValidator;
    private final PdfTextExtractor pdfTextExtractor;
    private final StatementFormatDetector statementFormatDetector;
    private final TransactionNormalizer transactionNormalizer;
    private final DescriptionCleanerRegistry descriptionCleanerRegistry;
    private final DuplicateDetectionService duplicateDetectionService;
    private final PayeeRuleService payeeRuleService;
    private final StorageService storageService;

    public StatementServiceImpl(
            StatementRepository statementRepository,
            StatementTransactionRepository statementTransactionRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            PdfValidator pdfValidator,
            PdfTextExtractor pdfTextExtractor,
            StatementFormatDetector statementFormatDetector,
            TransactionNormalizer transactionNormalizer,
            DescriptionCleanerRegistry descriptionCleanerRegistry,
            DuplicateDetectionService duplicateDetectionService,
            PayeeRuleService payeeRuleService,
            StorageService storageService) {
        this.statementRepository = statementRepository;
        this.statementTransactionRepository = statementTransactionRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.pdfValidator = pdfValidator;
        this.pdfTextExtractor = pdfTextExtractor;
        this.statementFormatDetector = statementFormatDetector;
        this.transactionNormalizer = transactionNormalizer;
        this.descriptionCleanerRegistry = descriptionCleanerRegistry;
        this.duplicateDetectionService = duplicateDetectionService;
        this.payeeRuleService = payeeRuleService;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public Statement upload(UUID userId, UploadStatementCommand command) {
        pdfValidator.validateUploadShape(command.content(), command.contentType());
        Account account = requireOwnedAccount(userId, command.accountId());
        String fileHash = sha256Hex(command.content());

        Optional<Statement> existing = statementRepository.findByUserIdAndFileHash(userId, fileHash);
        if (existing.isPresent()) {
            // file_hash gives upload-level idempotency (database-design.md) - re-uploading the
            // identical file returns the existing statement rather than reprocessing it...
            Statement previous = existing.get();
            if (previous.getStatus() == StatementStatus.FAILED) {
                // ...except a FAILED one: re-uploading is the natural way to try again after a fix, and the
                // stored copy may be gone (Render's disk is wiped on every redeploy), so the file is stored
                // again before it is reprocessed.
                storageService.store(previous.getStorageKey(), command.content());
                return process(userId, requireOwnedAccount(userId, previous.getAccountId()), previous);
            }
            return previous;
        }

        UUID statementId = UUID.randomUUID();
        String storageKey = userId + "/" + statementId + ".pdf";
        storageService.store(storageKey, command.content());

        Statement statement = new Statement(
                statementId, userId, account.getId(), command.fileName(), storageKey, fileHash, "application/pdf",
                null, null, StatementStatus.UPLOADED, null, Instant.now(), null);
        statement = statementRepository.save(statement);

        return process(userId, account, statement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Statement> list(UUID userId, Pageable pageable) {
        return statementRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Statement getOwned(UUID userId, UUID statementId) {
        return requireOwnedStatement(userId, statementId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatementTransaction> getTransactions(UUID userId, UUID statementId) {
        requireOwnedStatement(userId, statementId);
        return statementTransactionRepository.findByStatementIdAndUserId(statementId, userId);
    }

    @Override
    @Transactional
    public StatementTransaction updateStagedTransaction(
            UUID userId, UUID statementId, UUID stagingId, UpdateStagedTransactionCommand command) {
        requireOwnedStatement(userId, statementId);
        StatementTransaction row = requireOwnedStagedRow(userId, statementId, stagingId);
        row.applyReview(
                command.transactionDate(), command.amount(), command.merchantId(), command.categoryId(), command.transactionType());
        StatementTransaction saved = statementTransactionRepository.save(row);

        // Remember the choice, and apply it straight away to the other untouched rows of this statement
        // for the same payee, so the user sets "Rana Hyperstore" once rather than on every row.
        payeeRuleService.remember(userId, saved);
        List<StatementTransaction> siblings = statementTransactionRepository.findByStatementIdAndUserId(statementId, userId).stream()
                .filter(other -> !other.getId().equals(saved.getId()))
                .toList();
        statementTransactionRepository.saveAll(payeeRuleService.applyTo(userId, siblings));
        return saved;
    }

    @Override
    @Transactional
    public List<StatementTransaction> recheckDuplicates(UUID userId, UUID statementId) {
        Statement statement = requireOwnedStatement(userId, statementId);
        List<StatementTransaction> rows = statementTransactionRepository.findByStatementIdAndUserId(statementId, userId);
        if (statement.getStatus() != StatementStatus.READY_FOR_REVIEW) {
            return rows;
        }
        duplicateDetectionService.score(userId, statement.getAccountId(), rows);
        return statementTransactionRepository.saveAll(rows);
    }

    @Override
    @Transactional
    public StatementTransaction keepDuplicate(UUID userId, UUID statementId, UUID stagingId) {
        requireOwnedStatement(userId, statementId);
        StatementTransaction row = requireOwnedStagedRow(userId, statementId, stagingId);
        row.overrideDuplicate(Instant.now());
        return statementTransactionRepository.save(row);
    }

    @Override
    @Transactional
    public ConfirmResult confirm(UUID userId, UUID statementId) {
        Statement statement = requireOwnedStatement(userId, statementId);

        if (statement.getStatus() == StatementStatus.IMPORTED) {
            // A repeat confirm on an already-IMPORTED statement returns the original result
            // rather than importing twice (api-specification.md's explicit requirement).
            List<Transaction> alreadyImported = statementTransactionRepository.findByStatementIdAndUserId(statementId, userId).stream()
                    .filter(row -> row.getCanonicalTransactionId() != null)
                    .map(row -> transactionRepository.findByIdAndUserId(row.getCanonicalTransactionId(), userId)
                            .orElseThrow(() -> new StatementProcessingFailedException("A previously imported transaction is missing.")))
                    .toList();
            return new ConfirmResult(statement, alreadyImported, 0);
        }

        if (statement.getStatus() != StatementStatus.READY_FOR_REVIEW) {
            throw new DomainValidationException(
                    "Only a statement in READY_FOR_REVIEW can be confirmed (current status: " + statement.getStatus() + ").", List.of());
        }

        List<StatementTransaction> rows = statementTransactionRepository.findByStatementIdAndUserId(statementId, userId);
        // Re-scored here too, not just at staging: a statement staged before an overlapping one was
        // confirmed would otherwise carry no flags and double-count the shared transactions.
        duplicateDetectionService.score(userId, statement.getAccountId(), rows);
        List<Transaction> imported = new ArrayList<>();
        int skippedDuplicates = 0;
        for (StatementTransaction row : rows) {
            if (row.getReviewStatus() == ReviewStatus.REJECTED) {
                continue;
            }
            if (row.isUnresolvedDuplicate()) {
                statementTransactionRepository.save(row);
                skippedDuplicates++;
                continue;
            }
            Transaction transaction = new Transaction(
                    userId, statement.getAccountId(), row.getSuggestedMerchantId(), row.getSuggestedCategoryId(),
                    row.getTransactionDate(), row.getAmount(), row.getCurrency(), row.getNormalizedDescription(),
                    row.getRawDescription(), row.getSuggestedTransactionType(), TransactionSource.STATEMENT,
                    row.getExternalReference(), TransactionStatus.CONFIRMED);
            transaction = transactionRepository.save(transaction);
            row.markPromoted(transaction.getId());
            statementTransactionRepository.save(row);
            imported.add(transaction);
        }

        statement.markImported(Instant.now());
        statement = statementRepository.save(statement);
        return new ConfirmResult(statement, imported, skippedDuplicates);
    }

    @Override
    @Transactional
    public Statement retry(UUID userId, UUID statementId) {
        Statement statement = requireOwnedStatement(userId, statementId);
        if (statement.getStatus() != StatementStatus.FAILED) {
            throw new DomainValidationException("Only a FAILED statement can be retried (current status: " + statement.getStatus() + ").", List.of());
        }
        Account account = requireOwnedAccount(userId, statement.getAccountId());
        return process(userId, account, statement);
    }

    private Statement process(UUID userId, Account account, Statement statement) {
        statement.markProcessing();
        statement = statementRepository.save(statement);
        try {
            byte[] content;
            try {
                content = storageService.retrieve(statement.getStorageKey());
            } catch (UncheckedIOException e) {
                throw new StatementProcessingFailedException("The uploaded file is no longer stored - please upload it again.");
            }
            ExtractionResult extraction = pdfTextExtractor.extract(content);
            StatementParser parser = statementFormatDetector.detect(extraction.text());
            ParsedStatement parsed = parser.parse(extraction.text());
            if (parsed.rows().isEmpty()) {
                throw new StatementProcessingFailedException("No transactions could be parsed from this statement.");
            }
            UUID statementId = statement.getId();
            List<StatementTransaction> staged = parsed.rows().stream()
                    .map(row -> toStagedRow(userId, statementId, account.getCurrency(), parsed.detectedBank(), row))
                    .toList();
            payeeRuleService.applyTo(userId, staged);
            duplicateDetectionService.score(userId, account.getId(), staged);
            statementTransactionRepository.saveAll(staged);
            statement.markReadyForReview(parsed.periodStart(), parsed.periodEnd());
        } catch (ApiException e) {
            statement.markFailed(e.getMessage());
        }
        statementRepository.save(statement);
        // Re-fetched rather than returning the just-saved instance: @CreationTimestamp is only
        // populated on the entity once Hibernate actually flushes the original insert, which the
        // save() a few lines up in upload()/retry() does not force - the object in hand here can
        // still show createdAt as null even though the row in the database already has it set.
        // A fresh read forces that flush and returns the fully-populated row.
        return statementRepository.findByIdAndUserId(statement.getId(), userId)
                .orElseThrow(() -> new StatementProcessingFailedException("Statement disappeared during processing."));
    }

    private StatementTransaction toStagedRow(
            UUID userId, UUID statementId, String currency, String bankName, ParsedTransactionRow row) {
        NormalizedTransactionRow normalized = transactionNormalizer.normalize(row);
        String description = descriptionCleanerRegistry.clean(bankName, row.rawDescription(), normalized.normalizedDescription());
        return new StatementTransaction(
                userId, statementId, row.transactionDate(), row.amount(), currency, row.rawDescription(),
                description, null, null, normalized.transactionType(), normalized.confidenceScore(),
                row.sourceRowReference(), row.reference());
    }

    private Account requireOwnedAccount(UUID userId, UUID accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Account not found."));
    }

    private Statement requireOwnedStatement(UUID userId, UUID statementId) {
        return statementRepository.findByIdAndUserId(statementId, userId)
                .orElseThrow(() -> new NotFoundException("Statement not found."));
    }

    private StatementTransaction requireOwnedStagedRow(UUID userId, UUID statementId, UUID stagingId) {
        StatementTransaction row = statementTransactionRepository.findByIdAndUserId(stagingId, userId)
                .orElseThrow(() -> new NotFoundException("Staged transaction not found."));
        if (!row.getStatementId().equals(statementId)) {
            throw new NotFoundException("Staged transaction not found.");
        }
        return row;
    }

    private String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
