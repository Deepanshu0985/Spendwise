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
import com.finance.domain.transaction.TransactionLookup;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SearchTransactionsTool implements AssistantTool {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    private final TransactionSearch search;
    private final ObjectMapper mapper;

    public SearchTransactionsTool(TransactionSearch search, ObjectMapper mapper) {
        this.search = search;
        this.mapper = mapper;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("search_transactions",
                "Finds the user's individual transactions, including the biggest or smallest ones (sort AMOUNT_DESC or AMOUNT_ASC, type EXPENSE for spending). Use it to find or list specific payments (\"the payment to Sharma in March\","
                        + " \"my Uber rides\", \"expenses over 5000\"). Every filter is optional; all given filters must match. Shows at most 20"
                        + " rows and says how many matched in total. Do not use it to add up amounts - use aggregate for totals.",
                ToolSchemas.object(ToolSchemas.props(
                        "text", ToolSchemas.text("Words to look for in the description or merchant name, e.g. 'uber' or 'sharma traders'."),
                        "merchant", ToolSchemas.text("A merchant name or part of one."),
                        "category", ToolSchemas.text("The exact name of one of the user's categories."),
                        "type", ToolSchemas.text("EXPENSE, INCOME, REFUND, FEE_CHARGED, INTEREST_CHARGED, INTEREST_EARNED, TRANSFER_OUT, TRANSFER_IN or CASH_WITHDRAWAL."),
                        "from", ToolSchemas.date("Earliest date. Defaults to five years back."),
                        "to", ToolSchemas.date("Latest date. Defaults to today."),
                        "minAmount", ToolSchemas.number("Smallest amount."),
                        "maxAmount", ToolSchemas.number("Largest amount."),
                        "sort", ToolSchemas.text("DATE_DESC (newest first, the default), DATE_ASC, AMOUNT_DESC (largest first) or AMOUNT_ASC."),
                        "limit", ToolSchemas.integer("How many rows to show, 1 to 20. Default 10."))));
    }

    @Override
    @Transactional(readOnly = true)
    public ToolResult execute(UUID userId, String argumentsJson) {
        ToolArguments args = ToolArguments.parse(mapper, argumentsJson,
                "text", "merchant", "category", "type", "from", "to", "minAmount", "maxAmount", "sort", "limit");
        LocalDate today = search.today();
        LocalDate[] period = args.periodWithDefaults(today.minusYears(5).plusDays(1), today);
        TransactionSearch.Directory directory = search.directory(userId);
        TransactionSearch.Filters filters = search.parseFilters(args, directory, period, null);
        TransactionLookup.Sort sort = args.optionalEnum("sort", TransactionLookup.Sort.class);
        int limit = args.optionalInt("limit", DEFAULT_LIMIT, 1, MAX_LIMIT);

        TransactionSearch.Found found = search.search(userId, filters, directory, sort == null ? TransactionLookup.Sort.DATE_DESC : sort, limit);

        ObjectNode out = ToolOutput.object();
        ToolOutput.period(out, period[0], period[1]);
        out.put("totalMatches", found.totalMatches());
        out.put("shown", found.rows().size());
        ArrayNode rows = out.putArray("transactions");
        List<ToolResult.SourceRow> sources = new ArrayList<>();
        int number = 1;
        for (Transaction t : found.rows()) {
            ToolResult.SourceRow row = search.source(t, directory);
            sources.add(row);
            ObjectNode node = rows.addObject();
            node.put("result", number++);
            node.put("date", row.date().toString());
            node.put("description", UntrustedText.of(row.description()));
            if (row.merchant() != null) {
                node.put("merchant", UntrustedText.of(row.merchant()));
            }
            String category = directory.categoryName(t.getCategoryId());
            if (category != null) {
                node.put("category", UntrustedText.of(category));
            }
            node.put("type", row.type());
            node.put("amount", row.amount());
            node.put("currency", row.currency());
        }
        if (found.totalMatches() == 0) {
            out.put("note", "Nothing matched.");
        } else if (found.closeMatchesOnly()) {
            out.put("note", "Nothing matched exactly; these are close spellings. Say that they are close matches, not exact ones.");
        } else if (found.totalMatches() > found.rows().size()) {
            out.put("note", "Only " + found.rows().size() + " of " + found.totalMatches() + " matches are shown.");
        }
        return new ToolResult(out, "Transaction search, " + period[0] + " to " + period[1], sources);
    }
}
