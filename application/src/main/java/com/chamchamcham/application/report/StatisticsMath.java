package com.chamchamcham.application.report;

import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class StatisticsMath {
    private StatisticsMath() {}

    public static BigDecimal percentage(int numerator, int denominator) {
        return denominator > 0 ? BigDecimal.valueOf(numerator).multiply(new BigDecimal("100"))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP) : null;
    }

    public static BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        return denominator.compareTo(BigDecimal.ZERO) != 0 ? numerator.multiply(new BigDecimal("100"))
            .divide(denominator, 2, RoundingMode.HALF_UP) : null;
    }

    public static BigDecimal average(List<BigDecimal> values) {
        return values.isEmpty() ? null : values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal averageIntervalDays(List<LocalDate> dates) {
        var sorted = dates.stream().distinct().sorted().collect(Collectors.toCollection(ArrayList::new));
        if (sorted.size() < 2) return null;
        var intervals = new ArrayList<BigDecimal>();
        for (int i = 1; i < sorted.size(); i++) {
            intervals.add(BigDecimal.valueOf(ChronoUnit.DAYS.between(sorted.get(i - 1), sorted.get(i))));
        }
        return average(intervals);
    }
}
