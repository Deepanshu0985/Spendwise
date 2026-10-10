package com.finance.infrastructure.persistence.ai;

import com.finance.domain.ai.AiUsageRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Plain SQL on purpose: a reservation is one conditional UPDATE, which is what makes the caps race-free. Runs on the
 * request's own transaction connection, so the per-user table is still behind its tenant policy.
 */
@Repository
public class AiUsageRepositoryImpl implements AiUsageRepository {

    private final JdbcTemplate jdbc;

    public AiUsageRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean tryReserveDaily(UUID userId, LocalDate day, int rows, int limit) {
        jdbc.update("insert into ai_usage_daily (user_id, usage_date, rows_classified) values (?, ?, 0) on conflict do nothing",
                userId, Date.valueOf(day));
        return jdbc.update(
                "update ai_usage_daily set rows_classified = rows_classified + ? where user_id = ? and usage_date = ? and rows_classified + ? <= ?",
                rows, userId, Date.valueOf(day), rows, limit) == 1;
    }

    @Override
    public boolean tryReserveMonthly(LocalDate firstOfMonth, int rows, int limit) {
        jdbc.update("insert into ai_usage_monthly (usage_month, rows_classified) values (?, 0) on conflict do nothing", Date.valueOf(firstOfMonth));
        return jdbc.update(
                "update ai_usage_monthly set rows_classified = rows_classified + ? where usage_month = ? and rows_classified + ? <= ?",
                rows, Date.valueOf(firstOfMonth), rows, limit) == 1;
    }

    @Override
    public void releaseDaily(UUID userId, LocalDate day, int rows) {
        jdbc.update(
                "update ai_usage_daily set rows_classified = greatest(0, rows_classified - ?) where user_id = ? and usage_date = ?",
                rows, userId, Date.valueOf(day));
    }

    @Override
    public void releaseMonthly(LocalDate firstOfMonth, int rows) {
        jdbc.update("update ai_usage_monthly set rows_classified = greatest(0, rows_classified - ?) where usage_month = ?",
                rows, Date.valueOf(firstOfMonth));
    }

    @Override
    public int usedToday(UUID userId, LocalDate day) {
        return jdbc.query("select rows_classified from ai_usage_daily where user_id = ? and usage_date = ?",
                rs -> rs.next() ? rs.getInt(1) : 0, userId, Date.valueOf(day));
    }

    @Override
    public int usedThisMonth(LocalDate firstOfMonth) {
        return jdbc.query("select rows_classified from ai_usage_monthly where usage_month = ?",
                rs -> rs.next() ? rs.getInt(1) : 0, Date.valueOf(firstOfMonth));
    }
}
