package com.chamchamcham.domain.report;

import java.util.Objects;
import java.math.BigDecimal;

public record HarvestPartStatistics(
    String code,
    String label,
    int recordCount,
    BigDecimal recordRatePct,
    BigDecimal knownAmountKg,
    BigDecimal amountRatePct,
    Coverage amountCoverage
) {
    public HarvestPartStatistics {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(recordRatePct, "recordRatePct");
        Objects.requireNonNull(amountCoverage, "amountCoverage");
    }

}
