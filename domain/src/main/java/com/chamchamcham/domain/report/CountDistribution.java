package com.chamchamcham.domain.report;

import java.util.Objects;
import java.math.BigDecimal;

public record CountDistribution(String code, String label, int count, BigDecimal ratePct) {
    public CountDistribution {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(ratePct, "ratePct");
    }

}
