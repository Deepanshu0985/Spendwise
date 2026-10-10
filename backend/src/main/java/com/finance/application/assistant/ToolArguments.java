package com.finance.application.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads and validates the arguments a model supplied for a tool (ai-tools.md: schema-validated and range-checked before
 * execution). Unknown fields are rejected, not ignored - in particular a model that tries to pass a user id, or anything
 * else the tool does not declare, gets an error back instead of having it quietly dropped. Messages are written to be
 * read by the model so it can correct itself.
 */
public final class ToolArguments {

    /** A period longer than this is rejected rather than clamped: an unbounded range is never silently shortened. */
    public static final long MAX_PERIOD_DAYS = 5 * 366;

    private final JsonNode node;

    private ToolArguments(JsonNode node) {
        this.node = node;
    }

    public static ToolArguments parse(ObjectMapper mapper, String json, String... allowedFields) {
        JsonNode parsed;
        try {
            parsed = json == null || json.isBlank() ? mapper.createObjectNode() : mapper.readTree(json);
        } catch (Exception e) {
            throw new ToolArgumentException("The arguments were not valid JSON.");
        }
        if (!parsed.isObject()) {
            throw new ToolArgumentException("The arguments must be a JSON object.");
        }
        Set<String> allowed = new HashSet<>(Set.of(allowedFields));
        parsed.fieldNames().forEachRemaining(field -> {
            if (!allowed.contains(field)) {
                throw new ToolArgumentException("Unknown argument '" + field + "'. Allowed: " + String.join(", ", allowedFields) + ".");
            }
        });
        return new ToolArguments(parsed);
    }

    public String requiredText(String field, int maxLength) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new ToolArgumentException("'" + field + "' is required and must be text.");
        }
        if (value.asText().length() > maxLength) {
            throw new ToolArgumentException("'" + field + "' is too long.");
        }
        return value.asText().trim();
    }

    public String optionalText(String field, int maxLength) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual() || value.asText().length() > maxLength) {
            throw new ToolArgumentException("'" + field + "' must be short text.");
        }
        return value.asText().isBlank() ? null : value.asText().trim();
    }

    public boolean optionalBoolean(String field, boolean defaultValue) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        if (!value.isBoolean()) {
            throw new ToolArgumentException("'" + field + "' must be true or false.");
        }
        return value.asBoolean();
    }

    public int optionalInt(String field, int defaultValue, int min, int max) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        if (!value.isInt() || value.asInt() < min || value.asInt() > max) {
            throw new ToolArgumentException("'" + field + "' must be a whole number from " + min + " to " + max + ".");
        }
        return value.asInt();
    }

    /** from and to, both required ISO dates (yyyy-mm-dd), from not after to, and the span within the permitted bound. */
    public LocalDate[] period() {
        LocalDate from = date("from");
        LocalDate to = date("to");
        if (from.isAfter(to)) {
            throw new ToolArgumentException("'from' must not be after 'to'.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_PERIOD_DAYS) {
            throw new ToolArgumentException("The period is too long; ask for at most five years at a time.");
        }
        return new LocalDate[] {from, to};
    }

    private LocalDate date(String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw new ToolArgumentException("'" + field + "' is required, as a date written yyyy-mm-dd.");
        }
        try {
            return LocalDate.parse(value.asText());
        } catch (DateTimeParseException e) {
            throw new ToolArgumentException("'" + field + "' must be a real date written yyyy-mm-dd.");
        }
    }

    /** An optional date written yyyy-mm-dd, or null. */
    public LocalDate optionalDate(String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return date(field);
    }

    /** An optional money amount: a JSON number from 0 up to a sane ceiling, or null. */
    public BigDecimal optionalAmount(String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isNumber() || value.decimalValue().signum() < 0 || value.decimalValue().compareTo(new BigDecimal("1000000000000")) > 0) {
            throw new ToolArgumentException("'" + field + "' must be a number that is not negative.");
        }
        return value.decimalValue();
    }

    /** An optional value that must be one of the enum's names (case-insensitive), or null. */
    public <E extends Enum<E>> E optionalEnum(String field, Class<E> type) {
        String text = optionalText(field, 40);
        if (text == null) {
            return null;
        }
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(text)) {
                return constant;
            }
        }
        throw new ToolArgumentException("'" + field + "' must be one of: "
                + java.util.Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(java.util.stream.Collectors.joining(", ")) + ".");
    }

    /** Both ends of a period, with a default for whichever is missing, and the same bounds as period(). */
    public LocalDate[] periodWithDefaults(LocalDate defaultFrom, LocalDate defaultTo) {
        LocalDate from = optionalDate("from");
        LocalDate to = optionalDate("to");
        LocalDate effectiveTo = to != null ? to : defaultTo;
        LocalDate effectiveFrom = from != null ? from : (to != null ? to.minusYears(5).plusDays(1) : defaultFrom);
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new ToolArgumentException("'from' must not be after 'to'.");
        }
        if (ChronoUnit.DAYS.between(effectiveFrom, effectiveTo) > MAX_PERIOD_DAYS) {
            throw new ToolArgumentException("The period is too long; ask for at most five years at a time.");
        }
        return new LocalDate[] {effectiveFrom, effectiveTo};
    }

    /** A named period inside a multi-period request, e.g. prefix "first" reads firstFrom and firstTo. */
    public LocalDate[] namedPeriod(String prefix) {
        LocalDate from = date(prefix + "From");
        LocalDate to = date(prefix + "To");
        if (from.isAfter(to)) {
            throw new ToolArgumentException("'" + prefix + "From' must not be after '" + prefix + "To'.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_PERIOD_DAYS) {
            throw new ToolArgumentException("A period is too long; ask for at most five years at a time.");
        }
        return new LocalDate[] {from, to};
    }
}
