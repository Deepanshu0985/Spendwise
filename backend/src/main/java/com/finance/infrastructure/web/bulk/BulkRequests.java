package com.finance.infrastructure.web.bulk;

import com.finance.application.bulk.BulkCategorizationService;
import com.finance.application.statement.PayeeRuleService;
import com.finance.domain.statement.ReviewStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Request bodies for the bulk endpoints. The undo shapes are what a change returned, sent back as they were. */
public final class BulkRequests {

    private BulkRequests() {
    }

    public record StagedChangeRequest(
            @NotEmpty @Size(max = BulkCategorizationService.MAX_ROWS) List<@NotNull UUID> rowIds,
            UUID categoryId,
            UUID merchantId,
            boolean rememberPayee) {
    }

    public record LedgerChangeRequest(
            @NotEmpty @Size(max = BulkCategorizationService.MAX_ROWS) List<@NotNull UUID> transactionIds, UUID categoryId, UUID merchantId) {
    }

    public record RowSnapshotDto(@NotNull UUID id, UUID merchantId, UUID categoryId, @NotNull ReviewStatus reviewStatus, boolean aiSuggested, String aiReason) {
    }

    public record RuleSnapshotDto(@NotNull UUID id, UUID merchantId, UUID categoryId) {
    }

    public record RuleChangeDto(@Size(max = 1000) List<@NotNull UUID> createdRuleIds, @Size(max = 1000) List<@Valid RuleSnapshotDto> previousRules) {
    }

    public record StagedUndoRequest(@NotNull @Size(max = 1000) List<@Valid RowSnapshotDto> rows, @Valid RuleChangeDto rules) {

        public BulkCategorizationService.StagedUndo toUndo() {
            List<BulkCategorizationService.RowSnapshot> snapshots = rows.stream()
                    .map(r -> new BulkCategorizationService.RowSnapshot(r.id(), r.merchantId(), r.categoryId(), r.reviewStatus(), r.aiSuggested(), r.aiReason()))
                    .toList();
            PayeeRuleService.RuleChange change = rules == null
                    ? PayeeRuleService.RuleChange.none()
                    : new PayeeRuleService.RuleChange(
                            rules.createdRuleIds() == null ? List.of() : rules.createdRuleIds(),
                            rules.previousRules() == null
                                    ? List.of()
                                    : rules.previousRules().stream().map(r -> new PayeeRuleService.RuleSnapshot(r.id(), r.merchantId(), r.categoryId())).toList());
            return new BulkCategorizationService.StagedUndo(snapshots, change);
        }
    }

    public record TransactionSnapshotDto(@NotNull UUID id, UUID merchantId, UUID categoryId) {
    }

    public record LedgerUndoRequest(@NotNull @Size(max = BulkCategorizationService.MAX_ROWS) List<@Valid TransactionSnapshotDto> rows) {
    }
}
