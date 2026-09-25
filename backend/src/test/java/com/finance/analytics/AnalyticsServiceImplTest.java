package com.finance.analytics;

import com.finance.application.analytics.AnalyticsServiceImpl;
import com.finance.application.analytics.CategoryBreakdownView;
import com.finance.application.analytics.MonthlySummaryView;
import com.finance.application.exception.DomainValidationException;
import com.finance.domain.analytics.AnalyticsRepository;
import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionSource;
import com.finance.domain.transaction.TransactionStatus;
import com.finance.domain.transaction.TransactionType;
import com.finance.domain.transaction.TransactionWithSplits;
import com.finance.domain.user.User;
import com.finance.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The validation and currency-resolution logic that sits in front of
 * AnalyticsCalculator - mocked here (per coding-standards.md's stated
 * rationale for the interface-first rule) since AnalyticsRepository is a
 * genuine port with a real Postgres-backed implementation exercised
 * separately by AnalyticsFlowIT.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private AnalyticsRepository analyticsRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void rejectsAPeriodLongerThan36Months() {
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(analyticsRepository, userRepository);
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to = LocalDate.of(2023, 2, 1); // 38 months inclusive

        assertThatThrownBy(() -> service.monthlySummary(USER_ID, from, to, "INR"))
                .isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(analyticsRepository, userRepository);
    }

    @Test
    void rejectsToBeforeFrom() {
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(analyticsRepository, userRepository);
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(() -> service.monthlySummary(USER_ID, from, to, "INR"))
                .isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(analyticsRepository, userRepository);
    }

    @Test
    void defaultsToTheUsersDefaultCurrencyWhenNoneRequested() {
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(analyticsRepository, userRepository);
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        User user = new User("a@example.com", "hash", "Test User", "EUR", "Asia/Kolkata");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(analyticsRepository.findConfirmedResolvedTransactions(USER_ID, from, to)).thenReturn(List.of());

        MonthlySummaryView view = service.monthlySummary(USER_ID, from, to, null);

        assertThat(view.currency()).isEqualTo("EUR");
    }

    @Test
    void usesTheRequestedCurrencyWithoutConsultingTheUserWhenProvided() {
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(analyticsRepository, userRepository);
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        when(analyticsRepository.findConfirmedResolvedTransactions(USER_ID, from, to)).thenReturn(List.of());

        MonthlySummaryView view = service.monthlySummary(USER_ID, from, to, "usd");

        assertThat(view.currency()).isEqualTo("USD");
        verifyNoInteractions(userRepository);
    }

    @Test
    void excludesTransactionsInOtherCurrenciesAndReportsThem() {
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(analyticsRepository, userRepository);
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        Transaction inr = new Transaction(
                USER_ID, UUID.randomUUID(), null, null, LocalDate.of(2026, 9, 5), new BigDecimal("500"), "INR", "d",
                TransactionType.EXPENSE, TransactionSource.MANUAL, TransactionStatus.CONFIRMED);
        Transaction usd = new Transaction(
                USER_ID, UUID.randomUUID(), null, null, LocalDate.of(2026, 9, 6), new BigDecimal("10"), "USD", "d",
                TransactionType.EXPENSE, TransactionSource.MANUAL, TransactionStatus.CONFIRMED);

        when(analyticsRepository.findConfirmedResolvedTransactions(USER_ID, from, to)).thenReturn(
                List.of(new TransactionWithSplits(inr, List.of()), new TransactionWithSplits(usd, List.of())));
        when(analyticsRepository.categoryNamesForUser(USER_ID)).thenReturn(Map.of());

        CategoryBreakdownView view = service.categoryBreakdown(USER_ID, from, to, "INR");

        assertThat(view.exclusion().excludedCurrencies()).containsExactly("USD");
        assertThat(view.exclusion().excludedTransactionCount()).isEqualTo(1);
    }
}
