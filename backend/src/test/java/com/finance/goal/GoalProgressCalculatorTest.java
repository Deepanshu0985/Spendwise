package com.finance.goal;

import com.finance.domain.goal.Goal;
import com.finance.domain.goal.GoalProgress;
import com.finance.domain.goal.GoalProgressCalculator;
import com.finance.domain.goal.GoalStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GoalProgressCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    private static Goal goal(String target, String current, LocalDate date) {
        return new Goal(UUID.randomUUID(), "Laptop", new BigDecimal(target), new BigDecimal(current), date, "INR");
    }

    @Test
    void halfwayToATargetWithSixMonthsLeftNeedsTheRemainderSpreadOverThoseMonths() {
        GoalProgress progress = GoalProgressCalculator.calculate(goal("60000", "30000", LocalDate.of(2027, 4, 10)), TODAY);

        assertThat(progress.remaining()).isEqualByComparingTo("30000");
        assertThat(progress.percentComplete()).isEqualByComparingTo("50.0");
        assertThat(progress.requiredPerMonth()).isEqualByComparingTo("5000.00");
        assertThat(progress.overdue()).isFalse();
    }

    @Test
    void lessThanAMonthLeftNeedsTheWholeRemainderNow() {
        GoalProgress progress = GoalProgressCalculator.calculate(goal("10000", "4000", TODAY.plusDays(12)), TODAY);

        assertThat(progress.requiredPerMonth()).isEqualByComparingTo("6000.00");
    }

    @Test
    void theMonthlyAmountIsRoundedUpSoTheTargetIsActuallyReached() {
        GoalProgress progress = GoalProgressCalculator.calculate(goal("10000", "0", LocalDate.of(2027, 1, 10)), TODAY);

        assertThat(progress.requiredPerMonth()).isEqualByComparingTo("3333.34");
    }

    @Test
    void aPastTargetDateWithMoneyStillMissingIsOverdueWithNoMonthlyFigure() {
        GoalProgress progress = GoalProgressCalculator.calculate(goal("10000", "2000", TODAY.minusDays(1)), TODAY);

        assertThat(progress.overdue()).isTrue();
        assertThat(progress.requiredPerMonth()).isNull();
    }

    @Test
    void aGoalWithoutADateHasNoMonthlyFigure() {
        GoalProgress progress = GoalProgressCalculator.calculate(goal("10000", "2000", null), TODAY);

        assertThat(progress.requiredPerMonth()).isNull();
        assertThat(progress.overdue()).isFalse();
        assertThat(progress.percentComplete()).isEqualByComparingTo("20.0");
    }

    @Test
    void reachingTheTargetMarksTheGoalAchievedAndCapsTheFigures() {
        Goal goal = goal("10000", "2000", TODAY.plusMonths(3));
        goal.update("Laptop", new BigDecimal("10000"), new BigDecimal("12000"), TODAY.plusMonths(3));

        GoalProgress progress = GoalProgressCalculator.calculate(goal, TODAY);

        assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACHIEVED);
        assertThat(progress.remaining()).isEqualByComparingTo("0");
        assertThat(progress.percentComplete()).isEqualByComparingTo("100.0");
        assertThat(progress.requiredPerMonth()).isNull();
    }

    @Test
    void raisingTheTargetBeyondWhatIsSavedMakesTheGoalActiveAgain() {
        Goal goal = goal("10000", "10000", null);
        assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACHIEVED);

        goal.update("Laptop", new BigDecimal("15000"), new BigDecimal("10000"), null);
        assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACTIVE);
    }
}
