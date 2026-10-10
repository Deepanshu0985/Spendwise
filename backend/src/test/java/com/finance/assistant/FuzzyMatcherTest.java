package com.finance.assistant;

import com.finance.application.assistant.FuzzyMatcher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FuzzyMatcherTest {

    @Test
    void aSmallTypoInAWordOfTheTextIsClose() {
        assertThat(FuzzyMatcher.isClose("sharama", "UPI-SHARMA TRADERS")).isTrue();
        assertThat(FuzzyMatcher.isClose("starbuks", "STARBUCKS COFFEE 4821")).isTrue();
        assertThat(FuzzyMatcher.isClose("netflix", "Netflx India")).isTrue();
    }

    @Test
    void unrelatedWordsAreNotClose() {
        assertThat(FuzzyMatcher.isClose("sharma", "SWIGGY ORDER")).isFalse();
        assertThat(FuzzyMatcher.isClose("amazon", "AMAZING GRACE BOOKS")).isFalse();
    }

    @Test
    void shortWordsMustMatchExactlyElsewhereSoTheyNeverGuess() {
        assertThat(FuzzyMatcher.isClose("uber", "UBER INDIA")).isTrue(); // exact is handled by the exact search; length 4 allows one edit
        assertThat(FuzzyMatcher.isClose("ola", "OLD TOWN")).isFalse();
        assertThat(FuzzyMatcher.isClose("kfc", "KFC")).isFalse();
    }

    @Test
    void longWordsTolerateTwoEditsAndNoMore() {
        assertThat(FuzzyMatcher.isClose("electricity", "ELECTRICTY BILL")).isTrue();
        assertThat(FuzzyMatcher.isClose("electricity", "ELECTRIC BILL")).isFalse();
    }

    @Test
    void punctuationAndCaseDoNotMatter() {
        assertThat(FuzzyMatcher.isClose("SHARMA", "upi/sharma.traders@okaxis")).isTrue();
    }
}
