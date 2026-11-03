package com.chamchamcham.domain.report;

import java.util.Objects;
import java.math.BigDecimal;

public record CategoryMethodStatistics(
    String categoryCode,
    String categoryLabel,
    String methodCode,
    String methodLabel,
    int recordCount,
    BigDecimal recordRatePct
) {
    public CategoryMethodStatistics {
        Objects.requireNonNull(categoryCode, "categoryCode");
        Objects.requireNonNull(categoryLabel, "categoryLabel");
        Objects.requireNonNull(methodCode, "methodCode");
        Objects.requireNonNull(methodLabel, "methodLabel");
        Objects.requireNonNull(recordRatePct, "recordRatePct");
    }

}
