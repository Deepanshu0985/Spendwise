package com.finance.infrastructure.web.bulk;

import com.finance.application.bulk.BulkCategorizationService;
import com.finance.application.bulk.BulkCategorizationService.LedgerResult;
import com.finance.application.bulk.BulkCategorizationService.StagedResult;
import com.finance.application.statement.PayeeRuleService;
import com.finance.infrastructure.tenancy.TenantContext;
import com.finance.infrastructure.web.bulk.BulkRequests.LedgerChangeRequest;
import com.finance.infrastructure.web.bulk.BulkRequests.LedgerUndoRequest;
import com.finance.infrastructure.web.bulk.BulkRequests.StagedChangeRequest;
import com.finance.infrastructure.web.bulk.BulkRequests.StagedUndoRequest;
import com.finance.infrastructure.web.common.ApiResponse;
import com.finance.infrastructure.web.common.CurrentUserGuard;
import com.finance.infrastructure.web.statement.StatementTransactionResponse;
import com.finance.infrastructure.web.transaction.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Phase 16a: the same category (and/or merchant) on many rows at once, each with an undo. */
@RestController
public class BulkCategorizationController {

    public record StagedBulkResponse(List<StatementTransactionResponse> rows, int changedCount, BulkRequests.StagedUndoRequest undo) {
    }

    public record LedgerBulkResponse(List<TransactionResponse> changed, int skippedWithSplits, LedgerUndoRequest undo) {
    }

    private final BulkCategorizationService service;
    private final TenantContext tenantContext;

    public BulkCategorizationController(BulkCategorizationService service, TenantContext tenantContext) {
        this.service = service;
        this.tenantContext = tenantContext;
    }

    @PostMapping("/api/v1/statements/{id}/transactions/bulk")
    public ApiResponse<StagedBulkResponse> changeStagedRows(@PathVariable UUID id, @Valid @RequestBody StagedChangeRequest request) {
        StagedResult result = service.changeStagedRows(
                CurrentUserGuard.require(tenantContext), id,
                new BulkCategorizationService.StagedChange(request.rowIds(), request.categoryId(), request.merchantId(), request.rememberPayee()));
        return ApiResponse.of(new StagedBulkResponse(
                result.rows().stream().map(StatementTransactionResponse::from).toList(), result.changedCount(), undoDto(result.undo())));
    }

    @PostMapping("/api/v1/statements/{id}/transactions/bulk-restore")
    public ApiResponse<List<StatementTransactionResponse>> undoStagedRows(@PathVariable UUID id, @Valid @RequestBody StagedUndoRequest request) {
        return ApiResponse.of(service.undoStagedRows(CurrentUserGuard.require(tenantContext), id, request.toUndo()).stream()
                .map(StatementTransactionResponse::from).toList());
    }

    @PostMapping("/api/v1/transactions/bulk")
    public ApiResponse<LedgerBulkResponse> changeTransactions(@Valid @RequestBody LedgerChangeRequest request) {
        LedgerResult result = service.changeTransactions(
                CurrentUserGuard.require(tenantContext),
                new BulkCategorizationService.LedgerChange(request.transactionIds(), request.categoryId(), request.merchantId()));
        return ApiResponse.of(new LedgerBulkResponse(
                result.changed().stream().map(t -> TransactionResponse.from(t, List.of())).toList(),
                result.skippedWithSplits(),
                new LedgerUndoRequest(result.undo().stream().map(s -> new BulkRequests.TransactionSnapshotDto(s.id(), s.merchantId(), s.categoryId())).toList())));
    }

    @PostMapping("/api/v1/transactions/bulk-restore")
    public ApiResponse<List<TransactionResponse>> undoTransactions(@Valid @RequestBody LedgerUndoRequest request) {
        return ApiResponse.of(service.undoTransactions(
                        CurrentUserGuard.require(tenantContext),
                        request.rows().stream().map(r -> new BulkCategorizationService.TransactionSnapshot(r.id(), r.merchantId(), r.categoryId())).toList())
                .stream().map(t -> TransactionResponse.from(t, List.of())).toList());
    }

    private static StagedUndoRequest undoDto(BulkCategorizationService.StagedUndo undo) {
        PayeeRuleService.RuleChange rules = undo.rules();
        return new StagedUndoRequest(
                undo.rows().stream().map(r -> new BulkRequests.RowSnapshotDto(r.id(), r.merchantId(), r.categoryId(), r.reviewStatus(), r.aiSuggested(), r.aiReason())).toList(),
                new BulkRequests.RuleChangeDto(
                        rules.createdRuleIds(), rules.previousRules().stream().map(p -> new BulkRequests.RuleSnapshotDto(p.id(), p.merchantId(), p.categoryId())).toList()));
    }
}
