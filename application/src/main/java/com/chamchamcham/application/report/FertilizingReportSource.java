package com.chamchamcham.application.report;

import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record FertilizingReportSource(
    @NonNull String materialName,
    @NonNull BigDecimal amount,
    @NonNull String amountUnit,
    @Nullable BigDecimal amountKg,
    @Nullable CategoryRef applicationMethod
) {
    public FertilizingReportSource {
        Objects.requireNonNull(materialName, "materialName");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(amountUnit, "amountUnit");
    }

    @NonNull
    public String getMaterialName() { return materialName; }
    @NonNull
    public BigDecimal getAmount() { return amount; }
    @NonNull
    public String getAmountUnit() { return amountUnit; }
    @Nullable
    public BigDecimal getAmountKg() { return amountKg; }
    @Nullable
    public CategoryRef getApplicationMethod() { return applicationMethod; }
}
