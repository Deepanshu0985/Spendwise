package com.finance.recurring;

import com.finance.domain.recurring.RecurrenceDetector;
import com.finance.domain.recurring.RecurrenceFrequency;
import com.finance.domain.recurring.RecurringCandidate;
import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionSource;
import com.finance.domain.transaction.TransactionStatus;
import com.finance.domain.transaction.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceDetectorTest {

    private static final UUID USER = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    private static Transaction expense(String description, String date, String amount) {
        return expense(null, description, date, amount);
    }

    private static Transaction expense(UUID merchantId, String description, String date, String amount) {
        return new Transaction(
                USER, UUID.randomUUID(), merchantId, null, LocalDate.parse(date), new BigDecimal(amount), "INR", description,
                TransactionType.EXPENSE, TransactionSource.MANUAL, TransactionStatus.CONFIRMED);
    }

    private static List<Transaction> monthly(String description, String firstDate, int count, String amount) {
        List<Transaction> out = new ArrayList<>();
        LocalDate date = LocalDate.parse(firstDate);
        for (int i = 0; i < count; i++) {
            out.add(expense(description, date.plusMonths(i).toString(), amount));
        }
        return out;
    }

    @Test
    void aMonthlySubscriptionIsDetectedWithItsNextDateAndEstimates() {
        List<RecurringCandidate> found = RecurrenceDetector.detect(monthly("Netflix", "2026-06-05", 4, "649.00"), TODAY);

        assertThat(found).hasSize(1);
        RecurringCandidate netflix = found.get(0);
        assertThat(netflix.frequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
        assertThat(netflix.averageAmount()).isEqualByComparingTo("649.00");
        assertThat(netflix.lastSeenDate()).isEqualTo(LocalDate.of(2026, 9, 5));
        assertThat(netflix.nextExpectedDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(netflix.active()).isTrue();
        assertThat(netflix.occurrences()).isEqualTo(4);
        assertThat(netflix.confidence()).isGreaterThanOrEqualTo(new BigDecimal("0.60"));
    }

    @Test
    void twoPaymentsAreNotEnoughForAMonthlyPattern() {
        assertThat(RecurrenceDetector.detect(monthly("Gym", "2026-08-01", 2, "1500"), TODAY)).isEmpty();
    }

    @Test
    void aWeeklyPaymentWithTheSameAmountIsDetected() {
        List<Transaction> milk = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            milk.add(expense("Milk subscription", LocalDate.of(2026, 9, 1).plusWeeks(i).toString(), "120"));
        }
        List<RecurringCandidate> found = RecurrenceDetector.detect(milk, TODAY);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).frequency()).isEqualTo(RecurrenceFrequency.WEEKLY);
    }

    @Test
    void weeklyPaymentsWithVaryingAmountsAreAHabitNotASubscription() {
        List<Transaction> food = new ArrayList<>();
        String[] amounts = {"150", "480", "220", "610", "175"};
        for (int i = 0; i < 5; i++) {
            food.add(expense("Swiggy", LocalDate.of(2026, 9, 1).plusWeeks(i).toString(), amounts[i]));
        }
        assertThat(RecurrenceDetector.detect(food, TODAY)).isEmpty();
    }

    @Test
    void aFluctuatingMonthlyBillStillCountsButScoresLowerThanAFixedOne() {
        List<Transaction> bill = new ArrayList<>();
        String[] amounts = {"1800", "2100", "1950", "2300", "2000"};
        for (int i = 0; i < 5; i++) {
            bill.add(expense("Electricity bill", LocalDate.of(2026, 5, 12).plusMonths(i).toString(), amounts[i]));
        }
        RecurringCandidate variable = RecurrenceDetector.detect(bill, TODAY).get(0);
        RecurringCandidate fixed = RecurrenceDetector.detect(monthly("Netflix", "2026-05-12", 5, "649"), TODAY).get(0);

        assertThat(variable.frequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
        assertThat(variable.confidence()).isLessThan(fixed.confidence());
    }

    @Test
    void irregularGapsAreNotARecurringPattern() {
        List<Transaction> random = List.of(
                expense("Pharmacy", "2026-06-02", "300"), expense("Pharmacy", "2026-06-19", "300"),
                expense("Pharmacy", "2026-08-30", "300"), expense("Pharmacy", "2026-09-04", "300"));
        assertThat(RecurrenceDetector.detect(random, TODAY)).isEmpty();
    }

    @Test
    void aYearlyPaymentNeedsOnlyTwoOccurrences() {
        List<RecurringCandidate> found = RecurrenceDetector.detect(
                List.of(expense("Domain renewal", "2024-11-03", "1200"), expense("Domain renewal", "2025-11-03", "1200")),
                LocalDate.of(2026, 1, 15));

        assertThat(found).hasSize(1);
        assertThat(found.get(0).frequency()).isEqualTo(RecurrenceFrequency.YEARLY);
        assertThat(found.get(0).nextExpectedDate()).isEqualTo(LocalDate.of(2026, 11, 3));
    }

    @Test
    void aPatternThatStoppedLongAgoIsMarkedInactive() {
        List<RecurringCandidate> found = RecurrenceDetector.detect(monthly("Old OTT plan", "2026-01-10", 4, "299"), TODAY);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).active()).isFalse();
    }

    @Test
    void oneMissedMonthIsToleratedWhenThereIsEnoughHistory() {
        List<Transaction> rent = new ArrayList<>(monthly("Rent", "2026-01-01", 4, "15000"));
        rent.addAll(List.of(expense("Rent", "2026-06-01", "15000"), expense("Rent", "2026-07-01", "15000"), expense("Rent", "2026-08-01", "15000")));
        // gaps: 31 28 31 61 30 31 -> 5 of 6 regular
        assertThat(RecurrenceDetector.detect(rent, TODAY)).hasSize(1);
    }

    @Test
    void referenceNumbersInDescriptionsDoNotSplitOnePayeeIntoMany() {
        List<Transaction> transactions = List.of(
                expense("Spotify 8123456789", "2026-07-08", "119"), expense("Spotify 5567788990", "2026-08-08", "119"),
                expense("Spotify 1122334455", "2026-09-08", "119"));
        assertThat(RecurrenceDetector.detect(transactions, TODAY)).hasSize(1);
    }

    @Test
    void anAssignedMerchantGroupsPaymentsWhateverTheirDescriptionsSay() {
        UUID merchant = UUID.randomUUID();
        List<Transaction> transactions = List.of(
                expense(merchant, "Paid to Hotstar", "2026-07-02", "299"), expense(merchant, "HOTSTAR*SUB", "2026-08-02", "299"),
                expense(merchant, "UPI hotstar", "2026-09-02", "299"));
        List<RecurringCandidate> found = RecurrenceDetector.detect(transactions, TODAY);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).merchantId()).isEqualTo(merchant);
    }

    @Test
    void monthEndDatesDoNotDriftWhenProjectingTheNextPayment() {
        List<RecurringCandidate> found = RecurrenceDetector.detect(
                List.of(expense("Loan EMI", "2026-05-31", "8000"), expense("Loan EMI", "2026-06-30", "8000"),
                        expense("Loan EMI", "2026-07-31", "8000"), expense("Loan EMI", "2026-08-31", "8000")),
                TODAY);

        assertThat(found.get(0).nextExpectedDate()).isEqualTo(LocalDate.of(2026, 9, 30));
    }
}
