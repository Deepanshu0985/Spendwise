package com.finance.application.assistant.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finance.application.ai.ToolSpec;
import com.finance.application.analytics.AnalyticsService;
import com.finance.application.analytics.MonthlySummaryView;
import com.finance.application.assistant.AssistantTool;
import com.finance.application.assistant.ToolArguments;
import com.finance.application.assistant.ToolResult;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class MonthlySummaryTool implements AssistantTool {

    private final AnalyticsService analyticsService;
    private final ObjectMapper mapper;

    public MonthlySummaryTool(AnalyticsService analyticsService, ObjectMapper mapper) {
        this.analyticsService = analyticsService;
        this.mapper = mapper;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("monthly_summary",
                "Income, expenses, savings and savings rate for a period, in the user's currency. Use for questions about how much was earned, spent or saved.",
                ToolSchemas.object(ToolSchemas.props(
                        "from", ToolSchemas.date("First day of the period."), "to", ToolSchemas.date("Last day of the period.")), "from", "to"));
    }

    @Override
    public ToolResult execute(UUID userId, String argumentsJson) {
        LocalDate[] period = ToolArguments.parse(mapper, argumentsJson, "from", "to").period();
        MonthlySummaryView view = analyticsService.monthlySummary(userId, period[0], period[1], null);
        ObjectNode out = ToolOutput.object();
        out.put("currency", view.currency());
        ToolOutput.period(out, period[0], period[1]);
        out.put("income", ToolOutput.money(view.figures().income()));
        out.put("expenses", ToolOutput.money(view.figures().expenses()));
        out.put("savings", ToolOutput.money(view.figures().savings()));
        if (view.figures().savingsRate() == null) {
            out.putNull("savingsRate");
            out.put("note", "Savings rate is undefined because no income was recorded in this period.");
        } else {
            out.put("savingsRate", view.figures().savingsRate().setScale(1, java.math.RoundingMode.HALF_UP));
        }
        ToolOutput.exclusion(out, view.exclusion().excludedCurrencies(), view.exclusion().excludedTransactionCount());
        return new ToolResult(out, "Monthly summary, " + period[0] + " to " + period[1]);
    }
}
