package com.chamchamcham.application.report;

import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Set;
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
