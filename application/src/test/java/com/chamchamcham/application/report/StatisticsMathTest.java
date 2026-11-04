package com.chamchamcham.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.HarvestSource;
import org.junit.jupiter.api.DisplayName;

class StatisticsMathTest {

    @Test
    @DisplayName("percentage and average use scale two half up")
    void percentage_and_average_use_scale_two_half_up() throws Exception {
        assertThat(StatisticsMath.INSTANCE.percentage(1, 3)).isEqualByComparingTo("33.33");
        assertThat(StatisticsMath.INSTANCE.average(java.util.Arrays.asList(new BigDecimal("1"), new BigDecimal("2")))).isEqualByComparingTo("1.50");
    }

    @Test
    @DisplayName("average interval uses distinct dates")
    void average_interval_uses_distinct_dates() throws Exception {
        assertThat(StatisticsMath.INSTANCE.averageIntervalDays(java.util.Arrays.asList(date(1), date(1), date(4), date(10)))).isEqualByComparingTo("4.50");
    }

    @Test
    @DisplayName("empty values return null")
    void empty_values_return_null() throws Exception {
        assertThat(StatisticsMath.INSTANCE.percentage(0, 0)).isNull();
        assertThat(StatisticsMath.INSTANCE.average(List.of())).isNull();
        assertThat(StatisticsMath.INSTANCE.averageIntervalDays(List.of())).isNull();
    }
    @Test
    void decimalPercentagePreservesSignedDenominatorAndHalfUpRounding() {
        assertThat(StatisticsMath.INSTANCE.percentage(new BigDecimal("1"), new BigDecimal("-32"))).isEqualByComparingTo("-3.13");
        assertThat(StatisticsMath.INSTANCE.percentage(new BigDecimal("1"), new BigDecimal("0.00"))).isNull();
        assertThat(StatisticsMath.INSTANCE.average(List.of(new BigDecimal("0.005")))).isEqualByComparingTo("0.01");
    }

    private LocalDate date(int day) {
        return LocalDate.of(2026, 1, day);
    }
}
