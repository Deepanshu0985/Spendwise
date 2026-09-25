package com.finance.infrastructure.web.transaction;

import com.finance.application.transaction.CreateTransactionCommand;
import com.finance.application.transaction.CreateTransferCommand;
import com.finance.application.transaction.SplitCommand;
import com.finance.application.transaction.TransactionService;
import com.finance.application.transaction.TransferService;
import com.finance.application.transaction.UpdateTransactionCommand;
import com.finance.infrastructure.idempotency.IdempotencyService;
import com.finance.infrastructure.tenancy.TenantContext;
import com.finance.infrastructure.web.common.ApiResponse;
import com.finance.infrastructure.web.common.CurrentUserGuard;
import com.finance.infrastructure.web.common.PageMeta;
import com.finance.domain.transaction.TransactionFilter;
import com.finance.domain.transaction.TransactionStatus;
import com.finance.domain.transaction.TransactionType;
import com.finance.domain.transaction.TransactionWithSplits;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransferService transferService;
    private final TenantContext tenantContext;
    private final IdempotencyService idempotencyService;

    public TransactionController(
            TransactionService transactionService,
            TransferService transferService,
            TenantContext tenantContext,
            IdempotencyService idempotencyService) {
        this.transactionService = transactionService;
        this.transferService = transferService;
        this.tenantContext = tenantContext;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateTransactionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        UUID userId = CurrentUserGuard.require(tenantContext);
        return idempotencyService.executeIdempotent(userId, idempotencyKey, "POST /transactions", HttpStatus.OK, () -> {
            TransactionWithSplits created = transactionService.create(userId, toCommand(request));
            return TransactionResponse.from(created.transaction(), created.splits());
        });
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> createTransfer(
            @Valid @RequestBody CreateTransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        UUID userId = CurrentUserGuard.require(tenantContext);
        return idempotencyService.executeIdempotent(userId, idempotencyKey, "POST /transactions/transfer", HttpStatus.OK, () -> {
            TransferService.TransferResult result = transferService.create(
                    userId,
                    new CreateTransferCommand(
                            request.fromAccountId(), request.toAccountId(), request.transactionDate(), request.amount(), request.currency(),
                            request.kind()));
            return new TransferResponse(
                    TransactionResponse.from(result.outTransaction(), List.of()),
                    TransactionResponse.from(result.inTransaction(), List.of()));
        });
    }

    @GetMapping
    public ApiResponse<List<TransactionResponse>> list(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID merchantId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) String currency,
            Pageable pageable) {
        UUID userId = CurrentUserGuard.require(tenantContext);
        TransactionFilter filter = new TransactionFilter(from, to, accountId, categoryId, merchantId, type, status, currency);
        Page<TransactionWithSplits> page = transactionService.list(userId, filter, pageable);
        List<TransactionResponse> data = page.getContent().stream()
                .map(item -> TransactionResponse.from(item.transaction(), item.splits()))
                .toList();
        return ApiResponse.of(data, PageMeta.from(page));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> get(@PathVariable UUID id) {
        TransactionWithSplits found = transactionService.getOwned(CurrentUserGuard.require(tenantContext), id);
        return ApiResponse.of(TransactionResponse.from(found.transaction(), found.splits()));
    }

    @PutMapping("/{id}")
    public ApiResponse<TransactionResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateTransactionRequest request) {
        TransactionWithSplits updated = transactionService.update(CurrentUserGuard.require(tenantContext), id, toCommand(request));
        return ApiResponse.of(TransactionResponse.from(updated.transaction(), updated.splits()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        transactionService.softDelete(CurrentUserGuard.require(tenantContext), id);
        return ResponseEntity.noContent().build();
    }

    private CreateTransactionCommand toCommand(CreateTransactionRequest request) {
        return new CreateTransactionCommand(
                request.accountId(), request.merchantId(), request.categoryId(), request.transactionDate(), request.amount(),
                request.currency(), request.description(), request.transactionType(), toSplitCommands(request.splits()));
    }

    private UpdateTransactionCommand toCommand(UpdateTransactionRequest request) {
        return new UpdateTransactionCommand(
                request.accountId(), request.merchantId(), request.categoryId(), request.transactionDate(), request.amount(),
                request.description(), request.status(), toSplitCommands(request.splits()));
    }

    private List<SplitCommand> toSplitCommands(List<SplitRequest> splits) {
        if (splits == null) {
            return null;
        }
        return splits.stream().map(split -> new SplitCommand(split.categoryId(), split.amount())).toList();
    }
}
