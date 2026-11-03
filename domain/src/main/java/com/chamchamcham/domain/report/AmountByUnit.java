package com.chamchamcham.domain.report;

import java.util.Objects;
import java.math.BigDecimal;

public record AmountByUnit(String unit, BigDecimal amount, Coverage coverage) {
    public AmountByUnit {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(coverage, "coverage");
    }

}
