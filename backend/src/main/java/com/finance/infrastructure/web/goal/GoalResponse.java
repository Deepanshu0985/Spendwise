package com.finance.infrastructure.web.goal;

import com.finance.application.goal.GoalView;
import com.finance.domain.goal.Goal;
import com.finance.domain.goal.GoalProgress;
import com.finance.domain.goal.GoalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        String name,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        LocalDate targetDate,
        String currency,
        GoalStatus status,
        BigDecimal remaining,
        BigDecimal percentComplete,
        BigDecimal requiredPerMonth,
        boolean overdue) {

    public static GoalResponse from(GoalView view) {
        Goal goal = view.goal();
        GoalProgress progress = view.progress();
        return new GoalResponse(
                goal.getId(), goal.getName(), goal.getTargetAmount(), goal.getCurrentAmount(), goal.getTargetDate(), goal.getCurrency(),
                goal.getStatus(), progress.remaining(), progress.percentComplete(), progress.requiredPerMonth(), progress.overdue());
    }
}
