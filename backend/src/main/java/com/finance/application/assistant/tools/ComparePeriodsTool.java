package com.finance.application.assistant.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finance.application.ai.ToolSpec;
import com.finance.application.assistant.AssistantTool;
import com.finance.application.assistant.ToolArguments;
import com.finance.application.assistant.ToolResult;
import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

@Component
public class ComparePeriodsTool implements AssistantTool {

    private final TransactionSearch search;
    private final ObjectMapper mapper;

    public ComparePeriodsTool(TransactionSearch search, ObjectMapper mapper) {
        this.search = search;
        this.mapper = mapper;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("compare_periods",
                "Compares the total of matching transactions in two periods and returns both totals, the difference and the percentage change,"
                        + " all worked out here - never calculate them yourself. Use for \"how does this month compare with last month\" or"
                        + " \"more on food in September than August\". The second period is compared against the first (difference = second minus first).",
                ToolSchemas.object(ToolSchemas.props(
                        "firstFrom", ToolSchemas.date("Start of the first period."), "firstTo", ToolSchemas.date("End of the first period."),
                        "secondFrom", ToolSchemas.date("Start of the second period."), "secondTo", ToolSchemas.date("End of the second period."),
                        "type", ToolSchemas.text("Which type to add up: EXPENSE (default), INCOME, REFUND, FEE_CHARGED, INTEREST_CHARGED, INTEREST_EARNED, TRANSFER_OUT, TRANSFER_IN or CASH_WITHDRAWAL."),
                        "category", ToolSchemas.text("Only this category (exact name)."),
                        "merchant", ToolSchemas.text("Only this merchant (name or part of it)."),
                        "text", ToolSchemas.text("Only transactions with these words in the description or merchant name.")),
                        "firstFrom", "firstTo", "secondFrom", "secondTo"));
    }

    @Override
    @Transactional(readOnly = true)
    public ToolResult execute(UUID userId, String argumentsJson) {
        ToolArguments args = ToolArguments.parse(mapper, argumentsJson,
                "firstFrom", "firstTo", "secondFrom", "secondTo", "type", "category", "merchant", "text");
        LocalDate[] first = args.namedPeriod("first");
        LocalDate[] second = args.namedPeriod("second");
        TransactionSearch.Directory directory = search.directory(userId);
        TransactionSearch.Filters base = search.parseFilters(args, directory, new LocalDate[] {first[0], first[1]}, TransactionType.EXPENSE);
        TransactionSearch.Filters secondFilters = new TransactionSearch.Filters(
                second[0], second[1], base.minAmount(), base.maxAmount(), base.type(), base.categoryId(), base.words());

        List<Transaction> firstRows = search.all(userId, base, directory);
        List<Transaction> secondRows = search.all(userId, secondFilters, directory);
        Map<String, List<Transaction>> firstByCurrency = TransactionSearch.byCurrency(firstRows);
        Map<String, List<Transaction>> secondByCurrency = TransactionSearch.byCurrency(secondRows);

        ObjectNode out = ToolOutput.object();
        out.put("type", base.type().name());
        ObjectNode firstPeriod = out.putObject("firstPeriod");
        firstPeriod.put("from", first[0].toString());
        firstPeriod.put("to", first[1].toString());
        ObjectNode secondPeriod = out.putObject("secondPeriod");
        secondPeriod.put("from", second[0].toString());
        secondPeriod.put("to", second[1].toString());
        out.put("note", "Amounts are the sum of the matching transactions of this type; refunds are not netted off.");
        ArrayNode comparisons = out.putArray("comparisons");
        TreeSet<String> currencies = new TreeSet<>(firstByCurrency.keySet());
        currencies.addAll(secondByCurrency.keySet());
        for (String currency : currencies) {
            List<Transaction> a = firstByCurrency.getOrDefault(currency, new ArrayList<>());
            List<Transaction> b = secondByCurrency.getOrDefault(currency, new ArrayList<>());
            BigDecimal totalA = AggregateTransactionsTool.sum(a);
            BigDecimal totalB = AggregateTransactionsTool.sum(b);
            ObjectNode node = comparisons.addObject();
            node.put("currency", currency);
            node.put("firstTotal", ToolOutput.money(totalA));
            node.put("firstCount", a.size());
            node.put("secondTotal", ToolOutput.money(totalB));
            node.put("secondCount", b.size());
            node.put("difference", ToolOutput.money(totalB.subtract(totalA)));
            if (totalA.signum() > 0) {
                node.put("percentChange", totalB.subtract(totalA).multiply(BigDecimal.valueOf(100)).divide(totalA, 1, RoundingMode.HALF_UP));
            } else {
                node.putNull("percentChange");
                node.put("percentChangeNote", "Undefined because the first period's total is zero.");
            }
        }
        if (currencies.isEmpty()) {
            out.put("note", "Nothing matched in either period.");
        }
        return new ToolResult(out, "Comparison, " + first[0] + " to " + first[1] + " vs " + second[0] + " to " + second[1]);
    }
}
