package com.finance.infrastructure.persistence.user;

import com.finance.domain.user.UserDataRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Plain SQL on purpose: export and erasure must cover every table that holds a person's data, and a table added later
 * without a line here would leave data behind. UserDataIT checks that deleting the account (whose foreign keys refuse to
 * let the user row go while any other row still points at it) leaves nothing, so a forgotten table fails the build.
 */
@Repository
public class UserDataRepositoryImpl implements UserDataRepository {

    /** Table -> columns hidden from the export (internal ids that mean nothing to the person, or storage paths). */
    private static final Map<String, String> EXPORT_TABLES = new LinkedHashMap<>();

    static {
        EXPORT_TABLES.put("accounts", "");
        EXPORT_TABLES.put("categories", "");
        EXPORT_TABLES.put("merchants", "");
        EXPORT_TABLES.put("transactions", "");
        EXPORT_TABLES.put("transactionSplits", "");
        EXPORT_TABLES.put("statements", " - 'storage_key'");
        EXPORT_TABLES.put("statementRows", "");
        EXPORT_TABLES.put("payeeRules", "");
        EXPORT_TABLES.put("sourceAccountRules", "");
        EXPORT_TABLES.put("recurringExpenses", "");
        EXPORT_TABLES.put("budgets", "");
        EXPORT_TABLES.put("budgetCategories", "");
        EXPORT_TABLES.put("goals", "");
        EXPORT_TABLES.put("insights", "");
    }

    private static final Map<String, String> SQL_TABLE = Map.ofEntries(
            Map.entry("accounts", "accounts"),
            Map.entry("categories", "categories"),
            Map.entry("merchants", "merchants"),
            Map.entry("transactions", "transactions"),
            Map.entry("transactionSplits", "transaction_splits"),
            Map.entry("statements", "statements"),
            Map.entry("statementRows", "statement_transactions"),
            Map.entry("payeeRules", "payee_rules"),
            Map.entry("sourceAccountRules", "source_account_rules"),
            Map.entry("recurringExpenses", "recurring_expenses"),
            Map.entry("budgets", "budgets"),
            Map.entry("budgetCategories", "budget_categories"),
            Map.entry("goals", "goals"),
            Map.entry("insights", "ai_insights"));

    /** Children before parents, so no foreign key is ever left dangling mid-way. */
    private static final List<String> ERASE_ORDER = List.of(
            "transaction_splits",
            "statement_transactions",
            "statements",
            "source_account_rules",
            "payee_rules",
            "recurring_expenses",
            "budget_categories",
            "budgets",
            "goals",
            "ai_insights",
            "ai_usage_daily",
            "idempotency_keys",
            "transactions",
            "merchants",
            "categories",
            "accounts",
            "sessions",
            "password_reset_tokens");

    private final JdbcTemplate jdbcTemplate;

    public UserDataRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, String> exportTables(UUID userId) {
        Map<String, String> result = new LinkedHashMap<>();
        EXPORT_TABLES.forEach((key, hidden) -> {
            String sql = "SELECT coalesce(jsonb_agg(to_jsonb(t) - 'user_id'" + hidden + "), '[]'::jsonb)::text FROM "
                    + SQL_TABLE.get(key) + " t WHERE t.user_id = ?";
            result.put(key, jdbcTemplate.queryForObject(sql, String.class, userId));
        });
        return result;
    }

    @Override
    @Transactional
    public List<String> eraseAll(UUID userId) {
        List<String> storageKeys = jdbcTemplate.queryForList(
                "SELECT storage_key FROM statements WHERE user_id = ? AND storage_key IS NOT NULL", String.class, userId);
        for (String table : ERASE_ORDER) {
            jdbcTemplate.update("DELETE FROM " + table + " WHERE user_id = ?", userId);
        }
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        return storageKeys;
    }
}
