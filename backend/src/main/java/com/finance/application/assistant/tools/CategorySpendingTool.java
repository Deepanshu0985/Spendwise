package com.finance.application.assistant.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finance.application.ai.ToolSpec;
import com.finance.application.ai.UntrustedText;
import com.finance.application.analytics.AnalyticsService;
import com.finance.application.analytics.CategoryBreakdownView;
import com.finance.application.assistant.AssistantTool;
import com.finance.application.assistant.ToolArguments;
import com.finance.application.assistant.ToolResult;
import com.finance.domain.analytics.CategoryBreakdownEntry;
import com.finance.domain.category.Category;
import com.finance.domain.category.CategoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class CategorySpendingTool implements AssistantTool {

    private final AnalyticsService analyticsService;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper mapper;

    public CategorySpendingTool(AnalyticsService analyticsService, CategoryRepository categoryRepository, ObjectMapper mapper) {
        this.analyticsService = analyticsService;
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("category_spending",
                "How much was spent in one category (for example Food & Dining) over a period.",
                ToolSchemas.object(ToolSchemas.props(
                        "category", ToolSchemas.text("The category's name."),
                        "from", ToolSchemas.date("First day of the period."), "to", ToolSchemas.date("Last day of the period.")),
                        "category", "from", "to"));
    }

    @Override
    @Transactional(readOnly = true)
    public ToolResult execute(UUID userId, String argumentsJson) {
        ToolArguments args = ToolArguments.parse(mapper, argumentsJson, "category", "from", "to");
        String requested = args.requiredText("category", 100);
        LocalDate[] period = args.period();

        List<Category> categories = categoryRepository.findVisibleForUser(userId).stream().filter(Category::isActive).toList();
        Category match = categories.stream().filter(c -> c.getName().equalsIgnoreCase(requested)).findFirst().orElse(null);
        ObjectNode out = ToolOutput.object();
        if (match == null) {
            out.put("error", "No category with that name.");
            var names = out.putArray("availableCategories");
            categories.stream().map(c -> UntrustedText.of(c.getName())).sorted(String.CASE_INSENSITIVE_ORDER).forEach(names::add);
            return new ToolResult(out, "Category spending - unknown category");
        }
        CategoryBreakdownView view = analyticsService.categoryBreakdown(userId, period[0], period[1], null);
        BigDecimal amount = view.entries().stream()
                .filter(entry -> match.getId().equals(entry.categoryId()))
                .map(CategoryBreakdownEntry::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        out.put("currency", view.currency());
        out.put("category", UntrustedText.of(match.getName()));
        ToolOutput.period(out, period[0], period[1]);
        out.put("amount", ToolOutput.money(amount));
        ToolOutput.exclusion(out, view.exclusion().excludedCurrencies(), view.exclusion().excludedTransactionCount());
        return new ToolResult(out, match.getName().toLowerCase(Locale.ROOT) + " spending, " + period[0] + " to " + period[1]);
    }
}
