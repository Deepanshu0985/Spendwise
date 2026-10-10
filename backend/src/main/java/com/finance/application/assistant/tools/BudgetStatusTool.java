package com.finance.application.assistant.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finance.application.ai.ToolSpec;
import com.finance.application.ai.UntrustedText;
import com.finance.application.assistant.AssistantTool;
import com.finance.application.assistant.ToolArguments;
import com.finance.application.assistant.ToolResult;
import com.finance.application.budget.BudgetService;
import com.finance.application.budget.BudgetView;
import com.finance.domain.category.Category;
import com.finance.domain.category.CategoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class BudgetStatusTool implements AssistantTool {

    private final BudgetService budgetService;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper mapper;

    public BudgetStatusTool(BudgetService budgetService, CategoryRepository categoryRepository, ObjectMapper mapper) {
        this.budgetService = budgetService;
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("budget_status",
                "The user's budgets with limit, spent so far, remaining and percent used for the current period, in total and per category.",
                ToolSchemas.object(ToolSchemas.props("name", ToolSchemas.text("Only budgets whose name contains this text. Omit for all budgets."))));
    }

    @Override
    @Transactional(readOnly = true)
    public ToolResult execute(UUID userId, String argumentsJson) {
        String name = ToolArguments.parse(mapper, argumentsJson, "name").optionalText("name", 100);
        Map<UUID, String> categoryNames = categoryRepository.findVisibleForUser(userId).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        List<BudgetView> budgets = budgetService.list(userId).stream()
                .filter(view -> name == null || view.budget().getName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
                .toList();
        ObjectNode out = ToolOutput.object();
        ArrayNode list = out.putArray("budgets");
        for (BudgetView view : budgets) {
            ObjectNode node = list.addObject();
            node.put("name", UntrustedText.of(view.budget().getName()));
            node.put("currency", view.budget().getCurrency());
            ToolOutput.period(node, view.progress().windowStart(), view.progress().windowEnd());
            node.put("limit", ToolOutput.money(view.budget().getTotalLimit()));
            node.put("spent", ToolOutput.money(view.progress().totalSpent()));
            node.put("remaining", ToolOutput.money(view.progress().remaining()));
            node.put("percentUsed", view.progress().percentUsed());
            node.put("status", view.progress().status().name());
            ArrayNode categories = node.putArray("categories");
            view.progress().categories().forEach(line -> {
                ObjectNode c = categories.addObject();
                c.put("category", UntrustedText.of(categoryNames.getOrDefault(line.categoryId(), "Unknown")));
                c.put("limit", ToolOutput.money(line.limitAmount()));
                c.put("spent", ToolOutput.money(line.spent()));
                c.put("remaining", ToolOutput.money(line.remaining()));
                c.put("percentUsed", line.percentUsed());
                c.put("status", line.status().name());
            });
        }
        if (budgets.isEmpty()) {
            out.put("note", "No budgets found.");
        }
        return new ToolResult(out, "Budget status");
    }
}
