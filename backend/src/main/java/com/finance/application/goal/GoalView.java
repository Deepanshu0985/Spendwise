package com.finance.application.goal;

import com.finance.domain.goal.Goal;
import com.finance.domain.goal.GoalProgress;

public record GoalView(Goal goal, GoalProgress progress) {
}
