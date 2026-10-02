package com.finance.statement;

import com.finance.infrastructure.pdf.KeywordMerchantNameResolver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordMerchantNameResolverTest {

    private final KeywordMerchantNameResolver resolver = new KeywordMerchantNameResolver();

    @Test
    void recognisesKnownBrandsInsideHandlesCaseInsensitively() {
        assertThat(resolver.resolve("zomatoltd32.rzp")).contains("Zomato");
        assertThat(resolver.resolve("payzomato")).contains("Zomato");
        assertThat(resolver.resolve("BLINKIT.payu")).contains("Blinkit");
        assertThat(resolver.resolve("appleservices.bdsi")).contains("Apple");
    }

    @Test
    void leavesPeopleAndGenericMerchantHandlesUnresolved() {
        assertThat(resolver.resolve("7302904765")).isEmpty();
        assertThat(resolver.resolve("dhananjaybadoni")).isEmpty();
        assertThat(resolver.resolve("paytmqr6ux200")).isEmpty();
    }
}
