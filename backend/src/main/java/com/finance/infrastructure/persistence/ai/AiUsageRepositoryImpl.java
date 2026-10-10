package com.finance.infrastructure.persistence.ai;

import com.finance.domain.ai.AiUsageKind;
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
    public boolean tryReserveDaily(UUID userId, AiUsageKind kind, LocalDate day, int units, int limit) {
        jdbc.update(
                "insert into ai_usage_daily (user_id, usage_date, usage_kind, rows_classified) values (?, ?, ?, 0) on conflict do nothing",
                userId, Date.valueOf(day), kind.name());
        return jdbc.update(
                "update ai_usage_daily set rows_classified = rows_classified + ? where user_id = ? and usage_date = ? and usage_kind = ? "
                        + "and rows_classified + ? <= ?",
                units, userId, Date.valueOf(day), kind.name(), units, limit) == 1;
    }

    @Override
    public boolean tryReserveMonthly(AiUsageKind kind, LocalDate firstOfMonth, int units, int limit) {
        jdbc.update("insert into ai_usage_monthly (usage_month, usage_kind, rows_classified) values (?, ?, 0) on conflict do nothing",
                Date.valueOf(firstOfMonth), kind.name());
        return jdbc.update(
                "update ai_usage_monthly set rows_classified = rows_classified + ? where usage_month = ? and usage_kind = ? and rows_classified + ? <= ?",
                units, Date.valueOf(firstOfMonth), kind.name(), units, limit) == 1;
    }

    @Override
    public void releaseDaily(UUID userId, AiUsageKind kind, LocalDate day, int units) {
        jdbc.update(
                "update ai_usage_daily set rows_classified = greatest(0, rows_classified - ?) where user_id = ? and usage_date = ? and usage_kind = ?",
                units, userId, Date.valueOf(day), kind.name());
    }

    @Override
    public void releaseMonthly(AiUsageKind kind, LocalDate firstOfMonth, int units) {
        jdbc.update(
                "update ai_usage_monthly set rows_classified = greatest(0, rows_classified - ?) where usage_month = ? and usage_kind = ?",
                units, Date.valueOf(firstOfMonth), kind.name());
    }

    @Override
    public int usedToday(UUID userId, AiUsageKind kind, LocalDate day) {
        return jdbc.query("select rows_classified from ai_usage_daily where user_id = ? and usage_date = ? and usage_kind = ?",
                rs -> rs.next() ? rs.getInt(1) : 0, userId, Date.valueOf(day), kind.name());
    }

    @Override
    public int usedThisMonth(AiUsageKind kind, LocalDate firstOfMonth) {
        return jdbc.query("select rows_classified from ai_usage_monthly where usage_month = ? and usage_kind = ?",
                rs -> rs.next() ? rs.getInt(1) : 0, Date.valueOf(firstOfMonth), kind.name());
    }
}
