package com.finance.application.assistant.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finance.application.ai.ToolSpec;
import com.finance.application.analytics.AnalyticsService;
import com.finance.application.analytics.TrendView;
import com.finance.application.assistant.AssistantTool;
import com.finance.application.assistant.ToolArguments;
import com.finance.application.assistant.ToolResult;
import com.finance.domain.analytics.TrendPoint;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class SpendingTrendTool implements AssistantTool {

    private final AnalyticsService analyticsService;
    private final ObjectMapper mapper;

    public SpendingTrendTool(AnalyticsService analyticsService, ObjectMapper mapper) {
        this.analyticsService = analyticsService;
        this.mapper = mapper;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("spending_trend",
                "Month-by-month income, expenses and savings over a period. Use to compare months or describe how spending has changed.",
                ToolSchemas.object(ToolSchemas.props(
                        "from", ToolSchemas.date("First day of the period."), "to", ToolSchemas.date("Last day of the period."),
                        "grouping", ToolSchemas.text("Only 'month' is supported.")), "from", "to"));
    }

    @Override
    public ToolResult execute(UUID userId, String argumentsJson) {
        ToolArguments args = ToolArguments.parse(mapper, argumentsJson, "from", "to", "grouping");
        LocalDate[] period = args.period();
        String grouping = args.optionalText("grouping", 20);
        if (grouping != null && !grouping.equalsIgnoreCase("month")) {
            throw new com.finance.application.assistant.ToolArgumentException("Only grouping 'month' is supported.");
        }
        TrendView view = analyticsService.trend(userId, period[0], period[1], null);
        ObjectNode out = ToolOutput.object();
        out.put("currency", view.currency());
        ToolOutput.period(out, period[0], period[1]);
        ArrayNode months = out.putArray("months");
        for (TrendPoint point : view.points()) {
            ObjectNode item = months.addObject();
            item.put("month", point.month().toString());
            item.put("income", ToolOutput.money(point.figures().income()));
            item.put("expenses", ToolOutput.money(point.figures().expenses()));
            item.put("savings", ToolOutput.money(point.figures().savings()));
        }
        ToolOutput.exclusion(out, view.exclusion().excludedCurrencies(), view.exclusion().excludedTransactionCount());
        return new ToolResult(out, "Monthly trend, " + period[0] + " to " + period[1]);
    }
}
