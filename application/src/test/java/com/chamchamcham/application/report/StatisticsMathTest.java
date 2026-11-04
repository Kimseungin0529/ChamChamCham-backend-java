package com.chamchamcham.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;

class StatisticsMathTest {

    @Test
    @DisplayName("percentage and average use scale two half up")
    void percentageAndAverageUseScaleTwoHalfUp() throws Exception {
        assertThat(StatisticsMath.percentage(1, 3)).isEqualByComparingTo("33.33");
        assertThat(StatisticsMath.average(java.util.Arrays.asList(new BigDecimal("1"), new BigDecimal("2")))).isEqualByComparingTo("1.50");
    }

    @Test
    @DisplayName("average interval uses distinct dates")
    void averageIntervalUsesDistinctDates() throws Exception {
        assertThat(StatisticsMath.averageIntervalDays(java.util.Arrays.asList(date(1), date(1), date(4), date(10)))).isEqualByComparingTo("4.50");
    }

    @Test
    @DisplayName("empty values return null")
    void emptyValuesReturnNull() throws Exception {
        assertThat(StatisticsMath.percentage(0, 0)).isNull();
        assertThat(StatisticsMath.average(List.of())).isNull();
        assertThat(StatisticsMath.averageIntervalDays(List.of())).isNull();
    }
    @Test
    void decimalPercentagePreservesSignedDenominatorAndHalfUpRounding() {
        assertThat(StatisticsMath.percentage(new BigDecimal("1"), new BigDecimal("-32"))).isEqualByComparingTo("-3.13");
        assertThat(StatisticsMath.percentage(new BigDecimal("1"), new BigDecimal("0.00"))).isNull();
        assertThat(StatisticsMath.average(List.of(new BigDecimal("0.005")))).isEqualByComparingTo("0.01");
    }

    private LocalDate date(int day) {
        return LocalDate.of(2026, 1, day);
    }
}
