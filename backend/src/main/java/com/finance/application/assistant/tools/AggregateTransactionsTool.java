package com.finance.application.assistant.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finance.application.ai.ToolSpec;
import com.finance.application.ai.UntrustedText;
import com.finance.application.assistant.AssistantTool;
import com.finance.application.assistant.ToolArguments;
import com.finance.application.assistant.ToolResult;
import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class AggregateTransactionsTool implements AssistantTool {

    private static final int DEFAULT_GROUPS = 10;
    private static final int MAX_GROUPS = 20;

    enum GroupBy {
        NONE,
        CATEGORY,
        MERCHANT,
        MONTH,
        TYPE
    }

    private final TransactionSearch search;
    private final ObjectMapper mapper;

    public AggregateTransactionsTool(TransactionSearch search, ObjectMapper mapper) {
        this.search = search;
        this.mapper = mapper;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("aggregate",
                "Totals and counts of the user's transactions matching filters, optionally grouped by category, merchant, month or type."
                        + " Use for questions like \"how much did I spend at Uber this year\" or \"my spending by merchant\". Only one type is"
                        + " added up at a time (EXPENSE unless another is asked for) and refunds are not netted off; for the user's overall"
                        + " net spending in a period prefer monthly_summary or category_spending.",
                ToolSchemas.object(ToolSchemas.props(
                        "groupBy", ToolSchemas.text("NONE (one total, the default), CATEGORY, MERCHANT, MONTH or TYPE."),
                        "type", ToolSchemas.text("Which type to add up: EXPENSE (default), INCOME, REFUND, FEE_CHARGED, INTEREST_CHARGED, INTEREST_EARNED, TRANSFER_OUT, TRANSFER_IN or CASH_WITHDRAWAL."),
                        "text", ToolSchemas.text("Words to look for in the description or merchant name."),
                        "merchant", ToolSchemas.text("A merchant name or part of one."),
                        "category", ToolSchemas.text("The exact name of one of the user's categories."),
                        "from", ToolSchemas.date("Earliest date. Defaults to five years back."),
                        "to", ToolSchemas.date("Latest date. Defaults to today."),
                        "minAmount", ToolSchemas.number("Only transactions of at least this amount."),
                        "maxAmount", ToolSchemas.number("Only transactions of at most this amount."),
                        "groupLimit", ToolSchemas.integer("With grouping, how many groups to list, 1 to 20. Default 10."))));
    }

    @Override
    @Transactional(readOnly = true)
    public ToolResult execute(UUID userId, String argumentsJson) {
        ToolArguments args = ToolArguments.parse(mapper, argumentsJson,
                "groupBy", "type", "text", "merchant", "category", "from", "to", "minAmount", "maxAmount", "groupLimit");
        LocalDate today = search.today();
        LocalDate[] period = args.periodWithDefaults(today.minusYears(5).plusDays(1), today);
        TransactionSearch.Directory directory = search.directory(userId);
        TransactionSearch.Filters filters = search.parseFilters(args, directory, period, TransactionType.EXPENSE);
        GroupBy groupBy = args.optionalEnum("groupBy", GroupBy.class);
        groupBy = groupBy == null ? GroupBy.NONE : groupBy;
        int groupLimit = args.optionalInt("groupLimit", DEFAULT_GROUPS, 1, MAX_GROUPS);

        List<Transaction> rows = search.all(userId, filters, directory);

        ObjectNode out = ToolOutput.object();
        ToolOutput.period(out, period[0], period[1]);
        out.put("type", filters.type().name());
        out.put("groupBy", groupBy.name());
        out.put("note", "Amounts are the sum of the matching transactions of this type; refunds are not netted off.");
        ArrayNode totals = out.putArray("totals");
        // Currencies are never added together: each gets its own total.
        for (Map.Entry<String, List<Transaction>> entry : TransactionSearch.byCurrency(rows).entrySet()) {
            ObjectNode node = totals.addObject();
            node.put("currency", entry.getKey());
            node.put("total", ToolOutput.money(sum(entry.getValue())));
            node.put("count", entry.getValue().size());
            if (groupBy != GroupBy.NONE) {
                writeGroups(node, entry.getValue(), groupBy, groupLimit, directory);
            }
        }
        if (rows.isEmpty()) {
            out.put("note", "Nothing matched.");
        }
        return new ToolResult(out, "Totals" + (groupBy == GroupBy.NONE ? "" : " by " + groupBy.name().toLowerCase()) + ", " + period[0] + " to " + period[1]);
    }

    static BigDecimal sum(List<Transaction> rows) {
        return rows.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void writeGroups(ObjectNode node, List<Transaction> rows, GroupBy groupBy, int limit, TransactionSearch.Directory directory) {
        Function<Transaction, String> key = switch (groupBy) {
            case CATEGORY -> t -> {
                String name = directory.categoryName(t.getCategoryId());
                return name == null ? "Uncategorized" : UntrustedText.of(name);
            };
            case MERCHANT -> t -> {
                String name = directory.merchantName(t.getMerchantId());
                return name == null ? "Unknown merchant" : UntrustedText.of(name);
            };
            case MONTH -> t -> YearMonth.from(t.getTransactionDate()).toString();
            case TYPE -> t -> t.getTransactionType().name();
            case NONE -> t -> "";
        };
        Map<String, List<Transaction>> groups = new LinkedHashMap<>();
        for (Transaction t : rows) {
            groups.computeIfAbsent(key.apply(t), k -> new ArrayList<>()).add(t);
        }
        List<Map.Entry<String, List<Transaction>>> ordered = new ArrayList<>(groups.entrySet());
        if (groupBy == GroupBy.MONTH) {
            ordered.sort(Map.Entry.comparingByKey());
        } else {
            ordered.sort(Comparator.comparing((Map.Entry<String, List<Transaction>> e) -> sum(e.getValue())).reversed());
        }
        ArrayNode list = node.putArray("groups");
        ordered.stream().limit(limit).forEach(entry -> {
            ObjectNode g = list.addObject();
            g.put("key", entry.getKey());
            g.put("total", ToolOutput.money(sum(entry.getValue())));
            g.put("count", entry.getValue().size());
        });
        if (ordered.size() > limit) {
            node.put("groupsNotShown", ordered.size() - limit);
        }
    }
}
