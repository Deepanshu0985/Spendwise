package com.finance.application.assistant.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finance.application.ai.ToolSpec;
import com.finance.application.ai.UntrustedText;
import com.finance.application.assistant.AssistantTool;
import com.finance.application.assistant.ToolArguments;
import com.finance.application.assistant.ToolResult;
import com.finance.application.recurring.RecurringExpenseService;
import com.finance.domain.recurring.RecurringExpense;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RecurringExpensesTool implements AssistantTool {

    private static final int MAX_ITEMS = 25;

    private final RecurringExpenseService recurringExpenseService;
    private final ObjectMapper mapper;

    public RecurringExpensesTool(RecurringExpenseService recurringExpenseService, ObjectMapper mapper) {
        this.recurringExpenseService = recurringExpenseService;
        this.mapper = mapper;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("recurring_expenses",
                "The user's detected recurring payments (subscriptions, bills, EMIs) with how often and how much.",
                ToolSchemas.object(ToolSchemas.props("activeOnly", ToolSchemas.bool("Only payments that are still running. Default true."))));
    }

    @Override
    public ToolResult execute(UUID userId, String argumentsJson) {
        boolean activeOnly = ToolArguments.parse(mapper, argumentsJson, "activeOnly").optionalBoolean("activeOnly", true);
        List<RecurringExpense> items = recurringExpenseService.list(userId, activeOnly);
        ObjectNode out = ToolOutput.object();
        ArrayNode list = out.putArray("recurringExpenses");
        items.stream().limit(MAX_ITEMS).forEach(item -> {
            ObjectNode node = list.addObject();
            node.put("name", UntrustedText.of(item.getName()));
            node.put("currency", item.getCurrency());
            node.put("frequency", item.getFrequency().name());
            node.put("averageAmount", ToolOutput.money(item.getAverageAmount()));
            node.put("monthlyEstimate", ToolOutput.money(item.getMonthlyEstimate()));
            node.put("yearlyEstimate", ToolOutput.money(item.getYearlyEstimate()));
            node.put("nextExpectedDate", item.getNextExpectedDate().toString());
            node.put("stillRunning", item.isActive());
        });
        if (items.isEmpty()) {
            out.put("note", "No recurring payments have been detected yet.");
        }
        return new ToolResult(out, activeOnly ? "Active recurring payments" : "All recurring payments");
    }
}
