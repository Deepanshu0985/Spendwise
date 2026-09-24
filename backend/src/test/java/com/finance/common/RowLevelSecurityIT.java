package com.finance.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Raw JDBC, bypassing the application's own service/repository layer entirely
 * - these test the database-level RLS mechanism itself, the same way this
 * project's RLS behavior was originally hand-verified via direct psql (see
 * DECISIONS.md). Same webEnvironment as the other IT classes so Spring reuses
 * one booted context across all of them instead of booting twice.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class RowLevelSecurityIT {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void setLocalTenantContextDoesNotLeakAcrossPooledConnectionReuse() throws Exception {
        UUID userAId = createTestUser("rls-leak-a");

        // First "request": sets app.current_user_id via set_config(..., true),
        // exactly what TenantContextAspect does, then commits - which is
        // precisely when Postgres reverts a transaction-local setting.
        try (Connection first = dataSource.getConnection()) {
            first.setAutoCommit(false);
            setLocalUser(first, userAId);
            first.commit();
        }

        // Second "request": application-it.properties pins the pool to size 1,
        // so this is very likely the exact same physical connection - the
        // whole point of the test. No context is set, simulating an
        // unauthenticated request or a background job.
        try (Connection second = dataSource.getConnection()) {
            second.setAutoCommit(false);
            try (PreparedStatement ps = second.prepareStatement(
                            "SELECT NULLIF(current_setting('app.current_user_id', true), '') AS value");
                    ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("value"))
                        .as("a value from a prior committed transaction must not leak onto a reused connection")
                        .isNull();
            }
            second.commit();
        }
    }

    @Test
    void unsetTenantContextReturnsNoRowsInsteadOfErroring() throws Exception {
        // Regression test for the ''::uuid cast error found and fixed in
        // V7__fix_rls_policy_empty_string_guard.sql - see DECISIONS.md.
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement("SELECT count(*) FROM accounts");
                    ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isZero();
            }
            connection.commit();
        }
    }

    @Test
    void regularUserCannotModifyOrDeleteASystemCategory() throws Exception {
        UUID userId = createTestUser("rls-system-cat");
        UUID salaryId = jdbcTemplate.queryForObject(
                "SELECT id FROM categories WHERE name = 'Salary' AND is_system = true", UUID.class);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            setLocalUser(connection, userId);

            try (PreparedStatement update = connection.prepareStatement("UPDATE categories SET name = 'Hacked' WHERE id = ?")) {
                update.setObject(1, salaryId);
                assertThat(update.executeUpdate()).as("system rows are readable but never writable by a regular user").isZero();
            }

            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM categories WHERE id = ?")) {
                delete.setObject(1, salaryId);
                assertThat(delete.executeUpdate()).as("system rows are never deletable by a regular user").isZero();
            }
            connection.commit();
        }

        String nameAfter = jdbcTemplate.queryForObject("SELECT name FROM categories WHERE id = ?", String.class, salaryId);
        assertThat(nameAfter).isEqualTo("Salary");
    }

    private void setLocalUser(Connection connection, UUID userId) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("SELECT set_config('app.current_user_id', ?, true)")) {
            ps.setString(1, userId.toString());
            ps.execute();
        }
    }

    private UUID createTestUser(String label) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, full_name, default_currency, timezone) "
                        + "VALUES (?, ?, 'x', ?, 'INR', 'Asia/Kolkata')",
                id,
                label + "-" + UUID.randomUUID() + "@example.com",
                label);
        return id;
    }
}
