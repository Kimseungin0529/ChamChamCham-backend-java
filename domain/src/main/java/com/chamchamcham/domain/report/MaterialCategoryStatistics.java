package com.chamchamcham.domain.report;

import java.util.Objects;
import java.math.BigDecimal;

public record MaterialCategoryStatistics(
    String code,
    String label,
    int recordCount,
    BigDecimal recordRatePct,
    BigDecimal amountKg,
    BigDecimal amountRatePct
) {
    public MaterialCategoryStatistics {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(recordRatePct, "recordRatePct");
        Objects.requireNonNull(amountKg, "amountKg");
        Objects.requireNonNull(amountRatePct, "amountRatePct");
    }

}
