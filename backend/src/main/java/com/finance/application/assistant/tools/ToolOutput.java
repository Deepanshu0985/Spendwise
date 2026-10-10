package com.finance.application.assistant.tools;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** Builds tool results with money at two decimals and the period always spelled out, so an answer can state it. */
final class ToolOutput {

    static final JsonNodeFactory JSON = JsonNodeFactory.withExactBigDecimals(true);

    private ToolOutput() {
    }

    static ObjectNode object() {
        return JSON.objectNode();
    }

    static ArrayNode array() {
        return JSON.arrayNode();
    }

    static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    static void period(ObjectNode node, LocalDate from, LocalDate to) {
        ObjectNode period = node.putObject("period");
        period.put("from", from.toString());
        period.put("to", to.toString());
    }

    static void exclusion(ObjectNode node, java.util.List<String> excludedCurrencies, long excludedCount) {
        if (excludedCount > 0) {
            ArrayNode currencies = node.putArray("excludedCurrencies");
            excludedCurrencies.forEach(currencies::add);
            node.put("excludedTransactionCount", excludedCount);
        }
    }
}
