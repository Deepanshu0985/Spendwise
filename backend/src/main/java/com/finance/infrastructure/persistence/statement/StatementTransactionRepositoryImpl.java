package com.finance.infrastructure.persistence.statement;

import com.finance.domain.statement.StatementTransaction;
import com.finance.domain.statement.StatementTransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StatementTransactionRepositoryImpl implements StatementTransactionRepository {

    private final StatementTransactionJpaRepository jpaRepository;

    public StatementTransactionRepositoryImpl(StatementTransactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StatementTransaction save(StatementTransaction row) {
        StatementTransactionJpaEntity entity = jpaRepository.findById(row.getId())
                .map(existing -> StatementTransactionMapper.applyChanges(existing, row))
                .orElseGet(() -> StatementTransactionMapper.toNewEntity(row));
        return StatementTransactionMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<StatementTransaction> saveAll(List<StatementTransaction> rows) {
        List<StatementTransactionJpaEntity> entities = rows.stream().map(StatementTransactionMapper::toNewEntity).toList();
        return jpaRepository.saveAll(entities).stream().map(StatementTransactionMapper::toDomain).toList();
    }

    @Override
    public Optional<StatementTransaction> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(StatementTransactionMapper::toDomain);
    }

    @Override
    public List<StatementTransaction> findByStatementIdAndUserId(UUID statementId, UUID userId) {
        // Without an explicit order Postgres returns rows in physical order, and an UPDATE moves a row -
        // so an edited row would jump position on the review screen. Newest first, then statement order.
        return jpaRepository.findByStatementIdAndUserId(statementId, userId).stream()
                .map(StatementTransactionMapper::toDomain)
                .sorted(Comparator.comparing(StatementTransaction::getTransactionDate).reversed()
                        .thenComparingInt(row -> rowNumber(row.getSourceRowReference())))
                .toList();
    }

    /** "p1r12" -> 12; the parsers emit this as the row's position in the statement. */
    private static int rowNumber(String sourceRowReference) {
        int r = sourceRowReference.lastIndexOf('r');
        try {
            return Integer.parseInt(sourceRowReference.substring(r + 1));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return Integer.MAX_VALUE;
        }
    }
}
