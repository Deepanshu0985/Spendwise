package com.finance.insight;

import com.finance.domain.insight.UnusualSpendingDetector;
import com.finance.domain.insight.UnusualSpendingDetector.Kind;
import com.finance.domain.insight.UnusualSpendingDetector.Payment;
import com.finance.domain.insight.UnusualSpendingDetector.Unusual;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UnusualSpendingDetectorTest {

    private static BigDecimal d(String value) {
        return new BigDecimal(value);
    }

    private static final BigDecimal AVERAGE_SPEND = d("20000");

    private static List<Map<String, BigDecimal>> months(String food1, String food2, String food3) {
        return List.of(Map.of("Food", d(food1)), Map.of("Food", d(food2)), Map.of("Food", d(food3)));
    }

    private static List<Payment> payments(int count, String amount) {
        List<Payment> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new Payment("Shop visit " + i, LocalDate.of(2026, 9, 1).plusDays(i), d(amount)));
        }
        return list;
    }

    @Test
    void aCategoryWellAboveItsUsualMonthIsFlaggedWithTheNumbersWorkedOut() {
        List<Unusual> result = UnusualSpendingDetector.detect(
                Map.of("Food", d("5800")), months("3500", "3600", "3700"), AVERAGE_SPEND, List.of(), List.of());

        assertThat(result).hasSize(1);
        Unusual spike = result.get(0);
        assertThat(spike.kind()).isEqualTo(Kind.CATEGORY_SPIKE);
        assertThat(spike.label()).isEqualTo("Food");
        assertThat(spike.amount()).isEqualByComparingTo("5800.00");
        assertThat(spike.typical()).isEqualByComparingTo("3600.00");
        assertThat(spike.increase()).isEqualByComparingTo("2200.00");
        assertThat(spike.timesTypical()).isEqualByComparingTo("1.6");
    }

    @Test
    void aSlightRiseOrAMildOneIsNotFlagged() {
        // 1.4 times usual is below the 1.5 bar
        assertThat(UnusualSpendingDetector.detect(Map.of("Food", d("5040")), months("3600", "3600", "3600"), AVERAGE_SPEND, List.of(), List.of())).isEmpty();
        // 1.6 times usual but a rise of only 300 against a 20,000 month (under 5%) does not matter
        assertThat(UnusualSpendingDetector.detect(Map.of("Tea", d("800")), List.of(Map.of("Tea", d("500")), Map.of("Tea", d("500"))), AVERAGE_SPEND, List.of(), List.of())).isEmpty();
    }

    @Test
    void aCategoryWithTooLittleHistoryIsNeverCalledUnusual() {
        // only one earlier month of data: no baseline
        assertThat(UnusualSpendingDetector.detect(Map.of("Food", d("9000")), List.of(Map.of("Food", d("3000"))), AVERAGE_SPEND, List.of(), List.of())).isEmpty();
        // a category that did not exist before is not a spike either
        assertThat(UnusualSpendingDetector.detect(Map.of("Travel", d("9000")), months("100", "100", "100"), AVERAGE_SPEND, List.of(), List.of())).isEmpty();
    }

    @Test
    void theUsualLevelAveragesOnlyTheMonthsWhenThatCategoryHadSpending() {
        List<Map<String, BigDecimal>> history = List.of(Map.of("Food", d("4000")), Map.of("Other", d("10")), Map.of("Food", d("4000")));
        List<Unusual> result = UnusualSpendingDetector.detect(Map.of("Food", d("7000")), history, AVERAGE_SPEND, List.of(), List.of());

        assertThat(result.get(0).typical()).isEqualByComparingTo("4000.00");
    }

    @Test
    void aSinglePaymentFarAboveTheUsualOnesIsFlaggedOnceThereIsEnoughHistory() {
        List<Payment> thisMonth = List.of(new Payment("Laptop", LocalDate.of(2026, 10, 3), d("48000")), new Payment("Lunch", LocalDate.of(2026, 10, 4), d("300")));

        List<Unusual> result = UnusualSpendingDetector.detect(Map.of(), List.of(), AVERAGE_SPEND, thisMonth, payments(12, "400"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).kind()).isEqualTo(Kind.LARGE_PAYMENT);
        assertThat(result.get(0).label()).isEqualTo("Laptop");
        assertThat(result.get(0).typical()).isEqualByComparingTo("400.00");
        assertThat(result.get(0).timesTypical()).isEqualByComparingTo("120.0");
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2026, 10, 3));
    }

    @Test
    void largePaymentsNeedHistoryAndAMeaningfulSize() {
        List<Payment> thisMonth = List.of(new Payment("Big", LocalDate.of(2026, 10, 3), d("48000")));
        // fewer than ten earlier payments: no judgement
        assertThat(UnusualSpendingDetector.detect(Map.of(), List.of(), AVERAGE_SPEND, thisMonth, payments(9, "400"))).isEmpty();
        // three times the usual payment but tiny against the monthly scale
        List<Payment> small = List.of(new Payment("Snack", LocalDate.of(2026, 10, 3), d("90")));
        assertThat(UnusualSpendingDetector.detect(Map.of(), List.of(), AVERAGE_SPEND, small, payments(12, "20"))).isEmpty();
    }

    @Test
    void theMedianNotTheAverageSetsTheUsualPayment() {
        // one huge old payment must not drag the usual payment up and hide today's large one
        List<Payment> history = new ArrayList<>(payments(11, "400"));
        history.add(new Payment("Old house deposit", LocalDate.of(2026, 5, 1), d("500000")));
        List<Payment> thisMonth = List.of(new Payment("Laptop", LocalDate.of(2026, 10, 3), d("48000")));

        assertThat(UnusualSpendingDetector.detect(Map.of(), List.of(), AVERAGE_SPEND, thisMonth, history)).hasSize(1);
    }

    @Test
    void aRegularBigBillIsNotAnUnusualPayment() {
        // seen on real-looking data: rent and a monthly grocery order were reported as unusual every single month
        List<Payment> history = new ArrayList<>(payments(12, "400"));
        history.add(new Payment("Rent", LocalDate.of(2026, 9, 3), d("12000")));
        history.add(new Payment("Rent", LocalDate.of(2026, 8, 3), d("12000")));
        history.add(new Payment("BigBasket", LocalDate.of(2026, 9, 8), d("2100")));
        List<Payment> thisMonth = List.of(
                new Payment("Rent", LocalDate.of(2026, 10, 3), d("12000")),
                new Payment("RENT - October", LocalDate.of(2026, 10, 3), d("12000")),
                new Payment("Landlord transfer", LocalDate.of(2026, 10, 3), d("12000")), // same amount twice before: familiar
                new Payment("New laptop", LocalDate.of(2026, 10, 5), d("48000")));

        List<Unusual> result = UnusualSpendingDetector.detect(Map.of(), List.of(), AVERAGE_SPEND, thisMonth, history);

        assertThat(result).extracting(Unusual::label).containsExactly("New laptop");
    }

    @Test
    void numbersInADescriptionDoNotMakeARecurringPayeeLookNew() {
        List<Payment> history = new ArrayList<>(payments(12, "400"));
        history.add(new Payment("Cab ride 3", LocalDate.of(2026, 9, 3), d("9000")));
        List<Payment> thisMonth = List.of(new Payment("Cab ride 7", LocalDate.of(2026, 10, 3), d("9500")));

        assertThat(UnusualSpendingDetector.detect(Map.of(), List.of(), AVERAGE_SPEND, thisMonth, history)).isEmpty();
    }

    @Test
    void nothingIsFlaggedWithoutASpendingBaseline() {
        assertThat(UnusualSpendingDetector.detect(Map.of("Food", d("9000")), months("100", "100", "100"), BigDecimal.ZERO, List.of(), List.of())).isEmpty();
        assertThat(UnusualSpendingDetector.detect(Map.of("Food", d("9000")), months("100", "100", "100"), null, List.of(), List.of())).isEmpty();
    }

    @Test
    void resultsAreRankedByTheSizeOfTheRiseAndCapped() {
        Map<String, BigDecimal> current = new java.util.LinkedHashMap<>();
        List<Map<String, BigDecimal>> history = List.of(new java.util.HashMap<>(), new java.util.HashMap<>());
        for (int i = 1; i <= 7; i++) {
            current.put("Cat" + i, d(String.valueOf(2000 + i * 1000)));
            history.get(0).put("Cat" + i, d("1000"));
            history.get(1).put("Cat" + i, d("1000"));
        }

        List<Unusual> result = UnusualSpendingDetector.detect(current, history, AVERAGE_SPEND, List.of(), List.of());

        assertThat(result).hasSize(5);
        assertThat(result.get(0).label()).isEqualTo("Cat7");
        assertThat(result.get(4).label()).isEqualTo("Cat3");
    }
}
