package com.chamchamcham.domain.report;

import java.util.Objects;
import java.math.BigDecimal;

public record PropagationStatistics(
    String code,
    String label,
    int recordCount,
    BigDecimal recordRatePct,
    BigDecimal totalQuantity,
    String quantityUnit,
    Coverage quantityCoverage
) {
    public PropagationStatistics {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(recordRatePct, "recordRatePct");
        Objects.requireNonNull(quantityCoverage, "quantityCoverage");
    }

}
