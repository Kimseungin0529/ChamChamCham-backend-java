package com.chamchamcham.application.pesticide.sync;

import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record PsisPesticideRow(
    @NonNull String itemName,
    @NonNull String brandName,
    @NonNull String cropName,
    @NonNull String pestName,
    @Nullable String activeIngredient,
    @Nullable String formulation,
    @Nullable String usageCategory,
    @Nullable String humanToxicity,
    @Nullable String fishToxicity,
    @Nullable String manufacturer,
    @Nullable String dilutionRate,
    @Nullable String usageAmount,
    @Nullable String usageTiming,
    @Nullable String maxUsageCount
) {
    public PsisPesticideRow {
        Objects.requireNonNull(itemName, "itemName");
        Objects.requireNonNull(brandName, "brandName");
        Objects.requireNonNull(cropName, "cropName");
        Objects.requireNonNull(pestName, "pestName");
    }

    @NonNull
    public String getItemName() { return itemName; }
    @NonNull
    public String getBrandName() { return brandName; }
    @NonNull
    public String getCropName() { return cropName; }
    @NonNull
    public String getPestName() { return pestName; }
    @Nullable
    public String getActiveIngredient() { return activeIngredient; }
    @Nullable
    public String getFormulation() { return formulation; }
    @Nullable
    public String getUsageCategory() { return usageCategory; }
    @Nullable
    public String getHumanToxicity() { return humanToxicity; }
    @Nullable
    public String getFishToxicity() { return fishToxicity; }
    @Nullable
    public String getManufacturer() { return manufacturer; }
    @Nullable
    public String getDilutionRate() { return dilutionRate; }
    @Nullable
    public String getUsageAmount() { return usageAmount; }
    @Nullable
    public String getUsageTiming() { return usageTiming; }
    @Nullable
    public String getMaxUsageCount() { return maxUsageCount; }
}
