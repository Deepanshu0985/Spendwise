package com.finance.infrastructure.persistence.analytics;

import com.finance.domain.analytics.AnalyticsRepository;
import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionSource;
import com.finance.domain.transaction.TransactionSplit;
import com.finance.domain.transaction.TransactionStatus;
import com.finance.domain.transaction.TransactionType;
import com.finance.domain.transaction.TransactionWithSplits;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Plain JdbcTemplate, not a JPA entity - there's no "analytics row" to persist,
 * only reporting projections over transactions/transaction_splits/categories/
 * merchants, which every other feature already owns as JPA entities. Querying
 * those tables directly here (rather than reaching into infrastructure.persistence.
 * transaction/category/merchant's package-private JPA classes) keeps this
 * feature's adapter self-contained.
 */
@Repository
public class AnalyticsRepositoryImpl implements AnalyticsRepository {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TransactionWithSplits> findConfirmedResolvedTransactions(UUID userId, LocalDate from, LocalDate to) {
        // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
        List<Transaction> transactions = jdbcTemplate.query(
                "SELECT id, user_id, account_id, merchant_id, category_id, transaction_date, amount, currency, "
                        + "description, raw_description, transaction_type, payment_method, source, source_reference, "
                        + "external_transaction_id, transfer_group_id, confidence_score, status, created_at, updated_at "
                        + "FROM transactions "
                        + "WHERE user_id = ? AND status = 'CONFIRMED' AND transaction_type != 'UNKNOWN' "
                        + "AND transaction_date BETWEEN ? AND ?",
                AnalyticsRepositoryImpl::mapTransaction,
                userId, from, to);

        Map<UUID, List<TransactionSplit>> splitsByTransaction = jdbcTemplate.query(
                        "SELECT s.id, s.user_id, s.transaction_id, s.category_id, s.amount, s.created_at "
                                + "FROM transaction_splits s "
                                + "JOIN transactions t ON t.id = s.transaction_id "
                                + "WHERE s.user_id = ? AND t.status = 'CONFIRMED' AND t.transaction_type != 'UNKNOWN' "
                                + "AND t.transaction_date BETWEEN ? AND ?",
                        AnalyticsRepositoryImpl::mapSplit,
                        userId, from, to)
                .stream()
                .collect(Collectors.groupingBy(TransactionSplit::getTransactionId, LinkedHashMap::new, Collectors.toList()));

        return transactions.stream()
                .map(t -> new TransactionWithSplits(t, splitsByTransaction.getOrDefault(t.getId(), List.of())))
                .toList();
    }

    @Override
    public Map<UUID, String> categoryNamesForUser(UUID userId) {
        // is_system OR user_id = ? mirrors the categories_select RLS policy in application-layer form (defense-in-depth, not redundant with it).
        return queryNameMap("SELECT id, name FROM categories WHERE is_system = true OR user_id = ?", userId);
    }

    @Override
    public Map<UUID, String> merchantNamesForUser(UUID userId) {
        return queryNameMap("SELECT id, canonical_name FROM merchants WHERE user_id = ?", userId);
    }

    private Map<UUID, String> queryNameMap(String sql, UUID userId) {
        return jdbcTemplate.query(sql, rs -> {
            Map<UUID, String> names = new LinkedHashMap<>();
            while (rs.next()) {
                names.put(rs.getObject(1, UUID.class), rs.getString(2));
            }
            return names;
        }, userId);
    }

    private static Transaction mapTransaction(ResultSet rs, int rowNum) throws SQLException {
        return new Transaction(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getObject("account_id", UUID.class),
                rs.getObject("merchant_id", UUID.class),
                rs.getObject("category_id", UUID.class),
                rs.getDate("transaction_date").toLocalDate(),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getString("description"),
                rs.getString("raw_description"),
                TransactionType.valueOf(rs.getString("transaction_type")),
                rs.getString("payment_method"),
                TransactionSource.valueOf(rs.getString("source")),
                rs.getString("source_reference"),
                rs.getString("external_transaction_id"),
                rs.getObject("transfer_group_id", UUID.class),
                rs.getBigDecimal("confidence_score"),
                TransactionStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private static TransactionSplit mapSplit(ResultSet rs, int rowNum) throws SQLException {
        return new TransactionSplit(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getObject("transaction_id", UUID.class),
                rs.getObject("category_id", UUID.class),
                rs.getBigDecimal("amount"),
                rs.getTimestamp("created_at").toInstant());
    }
}
