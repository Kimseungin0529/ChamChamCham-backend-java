package com.chamchamcham.domain.report;

import java.util.Objects;
import java.math.BigDecimal;

public record CategoryAmountByUnit(
    String categoryCode,
    String categoryLabel,
    String unit,
    int recordCount,
    BigDecimal amount,
    Coverage coverage
) {
    public CategoryAmountByUnit {
        Objects.requireNonNull(categoryCode, "categoryCode");
        Objects.requireNonNull(categoryLabel, "categoryLabel");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(coverage, "coverage");
    }

}
