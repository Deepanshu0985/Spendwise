package com.finance.assistant;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantPeriodsTest {

    private static String guide(LocalDate today) throws Exception {
        Method method = Class.forName("com.finance.application.assistant.AssistantServiceImpl").getDeclaredMethod("periodsGuide", LocalDate.class);
        method.setAccessible(true);
        return (String) method.invoke(null, today);
    }

    @Test
    void relativePeriodsAreResolvedToExactDates() throws Exception {
        String guide = guide(LocalDate.of(2026, 10, 10));

        assertThat(guide).contains("this month: 2026-10-01 to 2026-10-31");
        assertThat(guide).contains("last month: 2026-09-01 to 2026-09-30");
        assertThat(guide).contains("the month before last: 2026-08-01 to 2026-08-31");
        assertThat(guide).contains("last 3 full months: 2026-07-01 to 2026-09-30");
        assertThat(guide).contains("last 6 full months: 2026-04-01 to 2026-09-30");
        assertThat(guide).contains("this year so far: 2026-01-01 to 2026-10-10");
        assertThat(guide).contains("last year: 2025-01-01 to 2025-12-31");
    }

    @Test
    void januaryAndLeapYearBoundariesAreRight() throws Exception {
        String january = guide(LocalDate.of(2027, 1, 15));
        assertThat(january).contains("last month: 2026-12-01 to 2026-12-31");
        assertThat(january).contains("the month before last: 2026-11-01 to 2026-11-30");
        assertThat(january).contains("last 3 full months: 2026-10-01 to 2026-12-31");

        String march = guide(LocalDate.of(2028, 3, 2));
        assertThat(march).contains("last month: 2028-02-01 to 2028-02-29");
        assertThat(march).contains("the month before last: 2028-01-01 to 2028-01-31");
    }
}
