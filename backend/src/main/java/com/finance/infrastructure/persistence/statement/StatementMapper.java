package com.finance.infrastructure.persistence.statement;

import com.finance.domain.statement.Statement;

/** Pure mapping logic, not swappable business behavior - no interface, same reasoning as CurrentUserGuard. */
final class StatementMapper {

    private StatementMapper() {
    }

    static Statement toDomain(StatementJpaEntity entity) {
        return new Statement(
                entity.getId(),
                entity.getUserId(),
                entity.getAccountId(),
                entity.getFileName(),
                entity.getStorageKey(),
                entity.getFileHash(),
                entity.getFileType(),
                entity.getPeriodStart(),
                entity.getPeriodEnd(),
                entity.getStatus(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getProcessedAt());
    }

    static StatementJpaEntity toNewEntity(Statement statement) {
        return new StatementJpaEntity(
                statement.getId(),
                statement.getUserId(),
                statement.getAccountId(),
                statement.getFileName(),
                statement.getStorageKey(),
                statement.getFileHash(),
                statement.getFileType(),
                statement.getPeriodStart(),
                statement.getPeriodEnd(),
                statement.getStatus(),
                statement.getErrorMessage(),
                statement.getProcessedAt());
    }

    /** Mutates an existing managed entity in place so Hibernate's dirty checking fires correctly. */
    static StatementJpaEntity applyChanges(StatementJpaEntity entity, Statement statement) {
        entity.setPeriodStart(statement.getPeriodStart());
        entity.setPeriodEnd(statement.getPeriodEnd());
        entity.setStatus(statement.getStatus());
        entity.setErrorMessage(statement.getErrorMessage());
        entity.setProcessedAt(statement.getProcessedAt());
        return entity;
    }
}
