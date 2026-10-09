package com.finance.infrastructure.persistence.statement;

import com.finance.domain.statement.Statement;
import com.finance.domain.statement.StatementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class StatementRepositoryImpl implements StatementRepository {

    private final StatementJpaRepository jpaRepository;

    public StatementRepositoryImpl(StatementJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Statement save(Statement statement) {
        StatementJpaEntity entity = jpaRepository.findById(statement.getId())
                .map(existing -> StatementMapper.applyChanges(existing, statement))
                .orElseGet(() -> StatementMapper.toNewEntity(statement));
        return StatementMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Statement> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(StatementMapper::toDomain);
    }

    @Override
    public Optional<Statement> findByUserIdAndFileHash(UUID userId, String fileHash) {
        return jpaRepository.findByUserIdAndFileHash(userId, fileHash).map(StatementMapper::toDomain);
    }

    @Override
    public Page<Statement> findByUserId(UUID userId, Pageable pageable) {
        // Without an explicit order Postgres returns rows in physical order, which an UPDATE reshuffles - newest
        // upload first is what the list should always show unless a caller asked for something else.
        Pageable ordered = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpaRepository.findByUserId(userId, ordered).map(StatementMapper::toDomain);
    }
}
