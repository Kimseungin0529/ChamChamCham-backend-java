package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.FertilizerAmountUnit;
import com.chamchamcham.domain.farming.FertilizingMethod;
import com.chamchamcham.domain.farming.HarvestSource;
import com.chamchamcham.domain.farming.IrrigationAmount;
import com.chamchamcham.domain.farming.IrrigationMethod;
import com.chamchamcham.domain.farming.PesticideAmountUnit;
import com.chamchamcham.domain.farming.PlantingMethod;
import com.chamchamcham.domain.farming.PropagationMethod;
import com.chamchamcham.domain.farming.SeedAmountUnit;
import com.chamchamcham.domain.farming.SeedlingUnit;
import com.chamchamcham.domain.farming.WeedingMethod;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record HarvestFeedbackDetail(
    @Nullable BigDecimal harvestAmount,
     boolean amountUnknown,
    @Nullable CropUsePartCategory medicinalPart,
    @NonNull HarvestSource harvestSource,
    @Nullable Integer growthPeriod,
    @Nullable String growthPeriodUnit,
    @com.fasterxml.jackson.annotation.JsonProperty("isLastHarvest") boolean isLastHarvest
) implements RecordFeedbackWorkDetail {
    public HarvestFeedbackDetail {
        Objects.requireNonNull(harvestSource, "harvestSource");
    }
    @Nullable public BigDecimal getHarvestAmount() { return harvestAmount; }
    public boolean getAmountUnknown() { return amountUnknown; }
    @Nullable public CropUsePartCategory getMedicinalPart() { return medicinalPart; }
    @NonNull public HarvestSource getHarvestSource() { return harvestSource; }
    @Nullable public Integer getGrowthPeriod() { return growthPeriod; }
    @Nullable public String getGrowthPeriodUnit() { return growthPeriodUnit; }
    @com.fasterxml.jackson.annotation.JsonProperty("isLastHarvest")
    public boolean isLastHarvest() { return isLastHarvest; }
}
