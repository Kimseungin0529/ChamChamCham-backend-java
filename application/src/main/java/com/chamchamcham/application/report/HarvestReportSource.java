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

public record HarvestReportSource(
    @Nullable BigDecimal amountKg,
    @Nullable CategoryRef medicinalPart,
    @NonNull CategoryRef harvestSource,
    @Nullable Integer growthPeriod,
    @Nullable String growthPeriodUnit,
    @Nullable Integer growthPeriodMonths,
    boolean isLastHarvest
) {
    public HarvestReportSource {
        Objects.requireNonNull(harvestSource, "harvestSource");
    }

    @Nullable
    public BigDecimal getAmountKg() { return amountKg; }
    @Nullable
    public CategoryRef getMedicinalPart() { return medicinalPart; }
    @NonNull
    public CategoryRef getHarvestSource() { return harvestSource; }
    @Nullable
    public Integer getGrowthPeriod() { return growthPeriod; }
    @Nullable
    public String getGrowthPeriodUnit() { return growthPeriodUnit; }
    @Nullable
    public Integer getGrowthPeriodMonths() { return growthPeriodMonths; }
    public boolean isLastHarvest() { return isLastHarvest; }
}
