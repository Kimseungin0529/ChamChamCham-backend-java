package com.chamchamcham.application.report;

import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record HarvestReportSource(
    @Nullable BigDecimal amountKg,
    @Nullable CategoryRef medicinalPart,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = HarvestSourceWithDefault.class)
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
    @com.fasterxml.jackson.annotation.JsonProperty("isLastHarvest")
    public boolean isLastHarvest() { return isLastHarvest; }

    public static class HarvestSourceWithDefault extends com.fasterxml.jackson.databind.JsonDeserializer<CategoryRef> {
        @Override
        public CategoryRef deserialize(com.fasterxml.jackson.core.JsonParser parser,
            com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, CategoryRef.class);
        }
        @Override public CategoryRef getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return new CategoryRef("CULTIVATED", "재배"); }
    }
}
