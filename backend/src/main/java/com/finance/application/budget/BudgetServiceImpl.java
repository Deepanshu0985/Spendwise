package com.finance.application.budget;

import com.finance.application.analytics.AnalyticsService;
import com.finance.application.exception.DomainValidationException;
import com.finance.application.exception.NotFoundException;
import com.finance.domain.analytics.CategoryBreakdownEntry;
import com.finance.domain.budget.Budget;
import com.finance.domain.budget.BudgetCategoryLimit;
import com.finance.domain.budget.BudgetPeriodType;
import com.finance.domain.budget.BudgetProgress;
import com.finance.domain.budget.BudgetProgressCalculator;
import com.finance.domain.budget.BudgetRepository;
import com.finance.domain.category.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final AnalyticsService analyticsService;
    private final Clock clock;

    public BudgetServiceImpl(
            BudgetRepository budgetRepository, CategoryRepository categoryRepository, AnalyticsService analyticsService, Clock clock) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.analyticsService = analyticsService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BudgetView create(UUID userId, SaveBudgetCommand command) {
        if (command.currency() == null || command.currency().isBlank()) {
            throw new DomainValidationException("A budget needs a currency.", List.of());
        }
        Window window = validatedWindow(command);
        Budget budget = new Budget(
                userId, command.name().trim(), command.periodType(), window.start(), window.end(), command.totalLimit(),
                command.currency().trim().toUpperCase(), validatedLimits(userId, command));
        return view(budgetRepository.save(budget));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetView> list(UUID userId) {
        return budgetRepository.findByUserId(userId).stream().map(this::view).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetView getOwned(UUID userId, UUID id) {
        return view(requireOwned(userId, id));
    }

    @Override
    @Transactional
    public BudgetView update(UUID userId, UUID id, SaveBudgetCommand command) {
        Budget budget = requireOwned(userId, id);
        Window window = validatedWindow(command);
        budget.update(command.name().trim(), command.periodType(), window.start(), window.end(), command.totalLimit(), validatedLimits(userId, command));
        return view(budgetRepository.save(budget));
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID id) {
        budgetRepository.delete(requireOwned(userId, id));
    }

    private Budget requireOwned(UUID userId, UUID id) {
        return budgetRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Budget not found."));
    }

    private BudgetView view(Budget budget) {
        LocalDate today = LocalDate.now(clock);
        LocalDate[] window = budget.windowAsOf(today);
        // The same figures the dashboard shows, so a budget and the dashboard can never disagree.
        Map<UUID, BigDecimal> spent = new HashMap<>();
        for (CategoryBreakdownEntry entry : analyticsService
                .categoryBreakdown(budget.getUserId(), window[0], window[1], budget.getCurrency()).entries()) {
            spent.merge(entry.categoryId(), entry.amount(), BigDecimal::add);
        }
        BudgetProgress progress = BudgetProgressCalculator.calculate(budget, today, spent);
        return new BudgetView(budget, progress);
    }

    private record Window(LocalDate start, LocalDate end) {
    }

    private Window validatedWindow(SaveBudgetCommand command) {
        if (command.periodType() == BudgetPeriodType.MONTHLY) {
            return new Window(LocalDate.now(clock).withDayOfMonth(1), null);
        }
        if (command.startDate() == null || command.endDate() == null) {
            throw new DomainValidationException("A custom budget needs a start and an end date.", List.of());
        }
        if (command.endDate().isBefore(command.startDate())) {
            throw new DomainValidationException("The end date can't be before the start date.", List.of());
        }
        return new Window(command.startDate(), command.endDate());
    }

    private List<BudgetCategoryLimit> validatedLimits(UUID userId, SaveBudgetCommand command) {
        List<BudgetCategoryLimitCommand> limits = command.categoryLimits() == null ? List.of() : command.categoryLimits();
        Set<UUID> seen = new HashSet<>();
        for (BudgetCategoryLimitCommand limit : limits) {
            if (!seen.add(limit.categoryId())) {
                throw new DomainValidationException("A category can only appear once in a budget.", List.of());
            }
            categoryRepository.findVisibleByIdForUser(limit.categoryId(), userId)
                    .orElseThrow(() -> new NotFoundException("Category not found."));
        }
        return limits.stream().map(limit -> new BudgetCategoryLimit(limit.categoryId(), limit.limitAmount())).toList();
    }
}
