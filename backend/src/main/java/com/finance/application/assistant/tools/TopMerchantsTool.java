package com.finance.application.assistant.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finance.application.ai.ToolSpec;
import com.finance.application.analytics.AnalyticsService;
import com.finance.application.analytics.MerchantBreakdownView;
import com.finance.application.assistant.AssistantTool;
import com.finance.application.assistant.ToolArguments;
import com.finance.application.assistant.ToolResult;
import com.finance.domain.analytics.MerchantBreakdownEntry;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;

@Component
public class TopMerchantsTool implements AssistantTool {

    private final AnalyticsService analyticsService;
    private final ObjectMapper mapper;

    public TopMerchantsTool(AnalyticsService analyticsService, ObjectMapper mapper) {
        this.analyticsService = analyticsService;
        this.mapper = mapper;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("top_merchants",
                "The merchants the user spent the most with over a period, largest first.",
                ToolSchemas.object(ToolSchemas.props(
                        "from", ToolSchemas.date("First day of the period."), "to", ToolSchemas.date("Last day of the period."),
                        "limit", ToolSchemas.integer("How many merchants to return, 1 to 10. Default 5.")), "from", "to"));
    }

    @Override
    public ToolResult execute(UUID userId, String argumentsJson) {
        ToolArguments args = ToolArguments.parse(mapper, argumentsJson, "from", "to", "limit");
        LocalDate[] period = args.period();
        int limit = args.optionalInt("limit", 5, 1, 10);
        MerchantBreakdownView view = analyticsService.merchantBreakdown(userId, period[0], period[1], null);
        ObjectNode out = ToolOutput.object();
        out.put("currency", view.currency());
        ToolOutput.period(out, period[0], period[1]);
        ArrayNode merchants = out.putArray("merchants");
        view.entries().stream()
                .sorted(Comparator.comparing(MerchantBreakdownEntry::amount).reversed())
                .limit(limit)
                .forEach(entry -> {
                    ObjectNode item = merchants.addObject();
                    item.put("merchant", UntrustedText.of(entry.merchantName()));
                    item.put("amount", ToolOutput.money(entry.amount()));
                });
        ToolOutput.exclusion(out, view.exclusion().excludedCurrencies(), view.exclusion().excludedTransactionCount());
        return new ToolResult(out, "Top merchants, " + period[0] + " to " + period[1]);
    }
}
