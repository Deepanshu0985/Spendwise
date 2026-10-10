package com.finance.ai;

import com.finance.domain.ai.PiiRedactor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiRedactorTest {

    @Test
    void phoneNumbersAndLongReferencesAreRemoved() {
        assertThat(PiiRedactor.redact("Paid to 9876543210")).isEqualTo("Paid to [number]");
        assertThat(PiiRedactor.redact("UPI Ref 615023242614 done")).isEqualTo("UPI Ref [number] done");
        assertThat(PiiRedactor.redact("call 98765 43210 now")).isEqualTo("call [number] now");
        assertThat(PiiRedactor.redact("card 4111-1111-1111-1111")).isEqualTo("card [number]");
    }

    @Test
    void shortNumbersThatCarryMeaningAreKept() {
        assertThat(PiiRedactor.redact("Order 4821 Pizza")).isEqualTo("Order 4821 Pizza");
        assertThat(PiiRedactor.redact("Bank Of Baroda - 21")).isEqualTo("Bank Of Baroda - 21");
    }

    @Test
    void theBankPartOfAUpiIdOrEmailGoesButTheMerchantNameStays() {
        assertThat(PiiRedactor.redact("zomatoltd32.rzp@icici")).isEqualTo("zomatoltd32.rzp");
        assertThat(PiiRedactor.redact("Paid to person@gmail.com for rent")).isEqualTo("Paid to person for rent");
        assertThat(PiiRedactor.redact("9876543210@ybl")).isEqualTo("[number]");
    }

    @Test
    void controlCharactersAreStrippedAndLengthIsCapped() {
        assertThat(PiiRedactor.redact("line1\nline2\u0000end")).isEqualTo("line1 line2 end");
        assertThat(PiiRedactor.redact("x".repeat(500))).hasSize(200);
        assertThat(PiiRedactor.redact(null)).isEmpty();
    }
}
