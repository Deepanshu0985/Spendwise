package com.finance.application.analytics;

import com.finance.application.exception.ApiError;
import com.finance.application.exception.DomainValidationException;
import com.finance.application.exception.NotFoundException;
import com.finance.domain.analytics.AnalyticsCalculator;
import com.finance.domain.analytics.AnalyticsRepository;
import com.finance.domain.analytics.CurrencyExclusion;
import com.finance.domain.analytics.TrendPoint;
import com.finance.domain.transaction.TransactionWithSplits;
import com.finance.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    // api-specification.md: "a period bounded to a maximum of 36 months", applied uniformly across all four endpoints.
    private static final int MAX_PERIOD_MONTHS = 36;

    private final AnalyticsRepository analyticsRepository;
    private final UserRepository userRepository;

    public AnalyticsServiceImpl(AnalyticsRepository analyticsRepository, UserRepository userRepository) {
        this.analyticsRepository = analyticsRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlySummaryView monthlySummary(UUID userId, LocalDate from, LocalDate to, String currency) {
        Period period = resolvePeriod(userId, from, to, currency);
        return new MonthlySummaryView(period.currency(), AnalyticsCalculator.figures(period.transactions()), period.exclusion());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryBreakdownView categoryBreakdown(UUID userId, LocalDate from, LocalDate to, String currency) {
        Period period = resolvePeriod(userId, from, to, currency);
        Map<UUID, String> categoryNames = analyticsRepository.categoryNamesForUser(userId);
        return new CategoryBreakdownView(
                period.currency(), AnalyticsCalculator.categoryBreakdown(period.transactions(), categoryNames), period.exclusion());
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantBreakdownView merchantBreakdown(UUID userId, LocalDate from, LocalDate to, String currency) {
        Period period = resolvePeriod(userId, from, to, currency);
        Map<UUID, String> merchantNames = analyticsRepository.merchantNamesForUser(userId);
        return new MerchantBreakdownView(
                period.currency(), AnalyticsCalculator.merchantBreakdown(period.transactions(), merchantNames), period.exclusion());
    }

    @Override
    @Transactional(readOnly = true)
    public TrendView trend(UUID userId, LocalDate from, LocalDate to, String currency) {
        Period period = resolvePeriod(userId, from, to, currency);
        List<TrendPoint> points = AnalyticsCalculator.trend(period.transactions(), YearMonth.from(from), YearMonth.from(to));
        return new TrendView(period.currency(), points, period.exclusion());
    }

    private Period resolvePeriod(UUID userId, LocalDate from, LocalDate to, String requestedCurrency) {
        requireValidPeriod(from, to);
        String currency = resolveCurrency(userId, requestedCurrency);

        List<TransactionWithSplits> all = analyticsRepository.findConfirmedResolvedTransactions(userId, from, to);
        List<TransactionWithSplits> matching = new ArrayList<>();
        Map<String, Long> excludedCounts = new LinkedHashMap<>();
        for (TransactionWithSplits tws : all) {
            String transactionCurrency = tws.transaction().getCurrency();
            if (transactionCurrency.equalsIgnoreCase(currency)) {
                matching.add(tws);
            } else {
                excludedCounts.merge(transactionCurrency, 1L, Long::sum);
            }
        }

        CurrencyExclusion exclusion = excludedCounts.isEmpty()
                ? CurrencyExclusion.NONE
                : new CurrencyExclusion(List.copyOf(excludedCounts.keySet()), excludedCounts.values().stream().mapToLong(Long::longValue).sum());

        return new Period(currency, matching, exclusion);
    }

    private void requireValidPeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new DomainValidationException(
                    "One or more fields are invalid.", List.of(new ApiError.Detail("from/to", "both are required")));
        }
        if (to.isBefore(from)) {
            throw new DomainValidationException(
                    "One or more fields are invalid.", List.of(new ApiError.Detail("to", "must not be before from")));
        }
        long months = ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to)) + 1;
        if (months > MAX_PERIOD_MONTHS) {
            throw new DomainValidationException(
                    "One or more fields are invalid.",
                    List.of(new ApiError.Detail("to", "period must not exceed " + MAX_PERIOD_MONTHS + " months")));
        }
    }

    private String resolveCurrency(UUID userId, String requestedCurrency) {
        if (requestedCurrency != null && !requestedCurrency.isBlank()) {
            return requestedCurrency.toUpperCase();
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."))
                .getDefaultCurrency();
    }

    private record Period(String currency, List<TransactionWithSplits> transactions, CurrencyExclusion exclusion) {
    }
}
