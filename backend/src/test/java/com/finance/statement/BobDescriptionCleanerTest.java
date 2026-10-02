package com.finance.statement;

import com.finance.infrastructure.pdf.BobDescriptionCleaner;
import com.finance.infrastructure.pdf.DescriptionCleaner;
import com.finance.infrastructure.pdf.DescriptionCleanerRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BobDescriptionCleanerTest {

    private final BobDescriptionCleaner cleaner = new BobDescriptionCleaner();

    @Test
    void stripsRefTimeAndBankSuffixDownToTheHandle() {
        assertThat(cleaner.clean("UPI/609609496741/18:05:55/UPI/zomatoltd32.rzp@hdf/Sent"))
                .contains("UPI: zomatoltd32.rzp");
    }

    @Test
    void keepsPhoneNumberHandlesAsTheyAre() {
        assertThat(cleaner.clean("UPI/209737101589/14:14:17/UPI/7302904765@ptyes/NA")).contains("UPI: 7302904765");
    }

    @Test
    void handlesNarrationWithoutABankSuffix() {
        assertThat(cleaner.clean("UPI/663200437791/18:22:31/UPI/UPILITE/LOAD/LITE")).contains("UPI: UPILITE LOAD/LITE".replace('/', ' '));
    }

    @Test
    void hasNoOpinionOnNonUpiNarration() {
        assertThat(cleaner.clean("ACHDR/EMIDUE/1234567890/111397544160")).isEmpty();
    }

    @Test
    void registryUsesTheBanksCleanerAndFallsBackOtherwise() {
        DescriptionCleanerRegistry registry = new DescriptionCleanerRegistry(List.<DescriptionCleaner>of(cleaner));

        assertThat(registry.clean("BOB", "UPI/1/10:00:00/UPI/swiggy@ybl/x", "fallback")).isEqualTo("UPI: swiggy");
        assertThat(registry.clean("BOB", "ACHDR/EMIDUE/1/2", "fallback")).isEqualTo("fallback");
        assertThat(registry.clean("PAYTM_WALLET", "UPI/1/10:00:00/UPI/swiggy@ybl/x", "fallback")).isEqualTo("fallback");
    }
}
