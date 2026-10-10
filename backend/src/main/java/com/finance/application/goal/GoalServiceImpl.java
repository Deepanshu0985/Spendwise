package com.finance.application.goal;

import com.finance.application.exception.DomainValidationException;
import com.finance.application.exception.NotFoundException;
import com.finance.domain.goal.Goal;
import com.finance.domain.goal.GoalProgressCalculator;
import com.finance.domain.goal.GoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final Clock clock;

    public GoalServiceImpl(GoalRepository goalRepository, Clock clock) {
        this.goalRepository = goalRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public GoalView create(UUID userId, SaveGoalCommand command) {
        if (command.currency() == null || command.currency().isBlank()) {
            throw new DomainValidationException("A goal needs a currency.", List.of());
        }
        BigDecimal current = current(command);
        Goal goal = new Goal(userId, command.name().trim(), command.targetAmount(), current, command.targetDate(), command.currency().trim().toUpperCase());
        return view(goalRepository.save(goal));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalView> list(UUID userId) {
        return goalRepository.findByUserId(userId).stream().map(this::view).toList();
    }

    @Override
    @Transactional
    public GoalView update(UUID userId, UUID id, SaveGoalCommand command) {
        Goal goal = goalRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Goal not found."));
        goal.update(command.name().trim(), command.targetAmount(), current(command), command.targetDate());
        return view(goalRepository.save(goal));
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID id) {
        goalRepository.delete(goalRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Goal not found.")));
    }

    private static BigDecimal current(SaveGoalCommand command) {
        BigDecimal current = command.currentAmount() == null ? BigDecimal.ZERO : command.currentAmount();
        if (current.signum() < 0) {
            throw new DomainValidationException("The amount saved can't be negative.", List.of());
        }
        return current;
    }

    private GoalView view(Goal goal) {
        return new GoalView(goal, GoalProgressCalculator.calculate(goal, LocalDate.now(clock)));
    }
}
