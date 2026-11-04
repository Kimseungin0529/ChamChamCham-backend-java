package com.chamchamcham.application.report;

import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record PlantingReportSource(
    @NonNull CategoryRef plantingMethod,
    @Nullable CategoryRef propagationMethod,
    @Nullable BigDecimal quantity,
    @Nullable String quantityUnit
) {
    public PlantingReportSource {
        Objects.requireNonNull(plantingMethod, "plantingMethod");
    }

    @NonNull
    public CategoryRef getPlantingMethod() { return plantingMethod; }
    @Nullable
    public CategoryRef getPropagationMethod() { return propagationMethod; }
    @Nullable
    public BigDecimal getQuantity() { return quantity; }
    @Nullable
    public String getQuantityUnit() { return quantityUnit; }
}
