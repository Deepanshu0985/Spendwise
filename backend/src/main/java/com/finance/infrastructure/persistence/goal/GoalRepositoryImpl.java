package com.finance.infrastructure.persistence.goal;

import com.finance.domain.goal.Goal;
import com.finance.domain.goal.GoalRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class GoalRepositoryImpl implements GoalRepository {

    private final GoalJpaRepository jpaRepository;

    public GoalRepositoryImpl(GoalJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Goal save(Goal goal) {
        GoalJpaEntity entity = jpaRepository.findById(goal.getId())
                .orElseGet(() -> new GoalJpaEntity(goal.getId(), goal.getUserId(), goal.getCurrency()));
        entity.setName(goal.getName());
        entity.setTargetAmount(goal.getTargetAmount());
        entity.setCurrentAmount(goal.getCurrentAmount());
        entity.setTargetDate(goal.getTargetDate());
        entity.setStatus(goal.getStatus());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Goal> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(GoalRepositoryImpl::toDomain);
    }

    @Override
    public List<Goal> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(GoalRepositoryImpl::toDomain).toList();
    }

    @Override
    public void delete(Goal goal) {
        jpaRepository.deleteById(goal.getId());
    }

    private static Goal toDomain(GoalJpaEntity e) {
        return new Goal(
                e.getId(), e.getUserId(), e.getName(), e.getTargetAmount(), e.getCurrentAmount(), e.getTargetDate(), e.getCurrency(),
                e.getStatus());
    }
}
