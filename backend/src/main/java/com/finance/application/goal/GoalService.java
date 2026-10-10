package com.finance.application.goal;

import java.util.List;
import java.util.UUID;

public interface GoalService {

    GoalView create(UUID userId, SaveGoalCommand command);

    List<GoalView> list(UUID userId);

    GoalView update(UUID userId, UUID id, SaveGoalCommand command);

    void delete(UUID userId, UUID id);
}
