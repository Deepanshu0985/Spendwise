package com.finance.infrastructure.web.statement;

import com.finance.application.statement.StatementService;
import com.finance.application.statement.UpdateStagedTransactionCommand;
import com.finance.application.statement.UploadStatementCommand;
import com.finance.domain.statement.Statement;
import com.finance.domain.statement.StatementTransaction;
import com.finance.infrastructure.idempotency.IdempotencyService;
import com.finance.infrastructure.tenancy.TenantContext;
import com.finance.infrastructure.web.common.ApiResponse;
import com.finance.infrastructure.web.common.CurrentUserGuard;
import com.finance.infrastructure.web.common.PageMeta;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/statements")
public class StatementController {

    private final StatementService statementService;
    private final TenantContext tenantContext;
    private final IdempotencyService idempotencyService;

    public StatementController(StatementService statementService, TenantContext tenantContext, IdempotencyService idempotencyService) {
        this.statementService = statementService;
        this.tenantContext = tenantContext;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ApiResponse<StatementResponse> upload(@RequestParam("file") MultipartFile file, @RequestParam UUID accountId) {
        UUID userId = CurrentUserGuard.require(tenantContext);
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded file.", e);
        }
        Statement statement = statementService.upload(
                userId, new UploadStatementCommand(accountId, file.getOriginalFilename(), file.getContentType(), content));
        return ApiResponse.of(StatementResponse.from(statement));
    }

    @GetMapping
    public ApiResponse<List<StatementResponse>> list(Pageable pageable) {
        UUID userId = CurrentUserGuard.require(tenantContext);
        Page<Statement> page = statementService.list(userId, pageable);
        List<StatementResponse> data = page.getContent().stream().map(StatementResponse::from).toList();
        return ApiResponse.of(data, PageMeta.from(page));
    }

    @GetMapping("/{id}")
    public ApiResponse<StatementResponse> get(@PathVariable UUID id) {
        Statement statement = statementService.getOwned(CurrentUserGuard.require(tenantContext), id);
        return ApiResponse.of(StatementResponse.from(statement));
    }

    @GetMapping("/{id}/transactions")
    public ApiResponse<List<StatementTransactionResponse>> getTransactions(@PathVariable UUID id) {
        List<StatementTransaction> rows = statementService.getTransactions(CurrentUserGuard.require(tenantContext), id);
        return ApiResponse.of(rows.stream().map(StatementTransactionResponse::from).toList());
    }

    @PutMapping("/{id}/transactions/{stagingId}")
    public ApiResponse<StatementTransactionResponse> updateStagedTransaction(
            @PathVariable UUID id, @PathVariable UUID stagingId, @Valid @RequestBody UpdateStagedTransactionRequest request) {
        UUID userId = CurrentUserGuard.require(tenantContext);
        StatementTransaction row = statementService.updateStagedTransaction(
                userId, id, stagingId,
                new UpdateStagedTransactionCommand(
                        request.transactionDate(), request.amount(), request.merchantId(), request.categoryId(), request.transactionType()));
        return ApiResponse.of(StatementTransactionResponse.from(row));
    }

    @PostMapping("/{id}/duplicates/recheck")
    public ApiResponse<List<StatementTransactionResponse>> recheckDuplicates(@PathVariable UUID id) {
        List<StatementTransaction> rows = statementService.recheckDuplicates(CurrentUserGuard.require(tenantContext), id);
        return ApiResponse.of(rows.stream().map(StatementTransactionResponse::from).toList());
    }

    @PostMapping("/{id}/transactions/{stagingId}/keep-duplicate")
    public ApiResponse<StatementTransactionResponse> keepDuplicate(@PathVariable UUID id, @PathVariable UUID stagingId) {
        StatementTransaction row = statementService.keepDuplicate(CurrentUserGuard.require(tenantContext), id, stagingId);
        return ApiResponse.of(StatementTransactionResponse.from(row));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<?> confirm(
            @PathVariable UUID id, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        UUID userId = CurrentUserGuard.require(tenantContext);
        return idempotencyService.executeIdempotent(
                userId, idempotencyKey, "POST /statements/" + id + "/confirm", HttpStatus.OK,
                () -> ConfirmResponse.from(statementService.confirm(userId, id)));
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<StatementResponse> retry(@PathVariable UUID id) {
        Statement statement = statementService.retry(CurrentUserGuard.require(tenantContext), id);
        return ApiResponse.of(StatementResponse.from(statement));
    }
}
