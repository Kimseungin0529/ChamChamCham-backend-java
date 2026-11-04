package com.chamchamcham.application.farming;

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
import com.chamchamcham.domain.farming.SprayAmountUnit;
import com.chamchamcham.domain.farming.WeedingMethod;
import com.chamchamcham.domain.farming.WorkType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class FarmingRecordResult {
    private FarmingRecordResult() {}

    public record RecordId(
        @NonNull UUID id,
        @NonNull WorkType workType
    ) {
        public RecordId {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(workType, "workType");
        }

        @NonNull
        public UUID getId() { return id; }
        @NonNull
        public WorkType getWorkType() { return workType; }
    }

    public record Summary(
        @NonNull UUID id,
        @NonNull UUID cropId,
        @NonNull String cropName,
        @NonNull WorkType workType,
        @NonNull LocalDateTime workedAt,
        @NonNull String weatherCondition,
        int weatherTemperature,
        @NonNull String memoPreview,
        @Nullable String thumbnailUrl,
        @Nullable IrrigationMethod irrigationMethod,
        @Nullable BigDecimal harvestAmount,
        @Nullable String pesticideName,
        @Nullable WeedingMethod weedingMethod,
        @Nullable PlantingMethod plantingMethod,
        @Nullable String materialName
    ) {
        public Summary {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
            Objects.requireNonNull(workType, "workType");
            Objects.requireNonNull(workedAt, "workedAt");
            Objects.requireNonNull(weatherCondition, "weatherCondition");
            Objects.requireNonNull(memoPreview, "memoPreview");
        }

        @NonNull
        public UUID getId() { return id; }
        @NonNull
        public UUID getCropId() { return cropId; }
        @NonNull
        public String getCropName() { return cropName; }
        @NonNull
        public WorkType getWorkType() { return workType; }
        @NonNull
        public LocalDateTime getWorkedAt() { return workedAt; }
        @NonNull
        public String getWeatherCondition() { return weatherCondition; }
        public int getWeatherTemperature() { return weatherTemperature; }
        @NonNull
        public String getMemoPreview() { return memoPreview; }
        @Nullable
        public String getThumbnailUrl() { return thumbnailUrl; }
        @Nullable
        public IrrigationMethod getIrrigationMethod() { return irrigationMethod; }
        @Nullable
        public BigDecimal getHarvestAmount() { return harvestAmount; }
        @Nullable
        public String getPesticideName() { return pesticideName; }
        @Nullable
        public WeedingMethod getWeedingMethod() { return weedingMethod; }
        @Nullable
        public PlantingMethod getPlantingMethod() { return plantingMethod; }
        @Nullable
        public String getMaterialName() { return materialName; }
    }

    public record Page(
        @NonNull List<Summary> items,
        @Nullable String nextCursor
    ) {
        public Page {
            Objects.requireNonNull(items, "items");
        }

        @NonNull
        public List<Summary> getItems() { return items; }
        @Nullable
        public String getNextCursor() { return nextCursor; }
    }

    public record Detail(
        @NonNull UUID id,
        @NonNull UUID farmId,
        @NonNull String farmName,
        @NonNull UUID cropId,
        @NonNull String cropName,
        @NonNull WorkType workType,
        @NonNull LocalDateTime workedAt,
        @NonNull String weatherCondition,
        int weatherTemperature,
        @NonNull String memo,
        @Nullable PlantingDetail planting,
        @Nullable WateringDetail watering,
        @Nullable FertilizingDetail fertilizing,
        @Nullable PestControlDetail pestControl,
        @Nullable WeedingDetail weeding,
        @Nullable HarvestDetail harvest,
        @NonNull List<MediaItem> images,
        @NonNull LocalDateTime createdAt,
        @NonNull LocalDateTime updatedAt
    ) {
        public Detail {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(farmName, "farmName");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
            Objects.requireNonNull(workType, "workType");
            Objects.requireNonNull(workedAt, "workedAt");
            Objects.requireNonNull(weatherCondition, "weatherCondition");
            Objects.requireNonNull(memo, "memo");
            Objects.requireNonNull(images, "images");
            Objects.requireNonNull(createdAt, "createdAt");
            Objects.requireNonNull(updatedAt, "updatedAt");
        }

        @NonNull
        public UUID getId() { return id; }
        @NonNull
        public UUID getFarmId() { return farmId; }
        @NonNull
        public String getFarmName() { return farmName; }
        @NonNull
        public UUID getCropId() { return cropId; }
        @NonNull
        public String getCropName() { return cropName; }
        @NonNull
        public WorkType getWorkType() { return workType; }
        @NonNull
        public LocalDateTime getWorkedAt() { return workedAt; }
        @NonNull
        public String getWeatherCondition() { return weatherCondition; }
        public int getWeatherTemperature() { return weatherTemperature; }
        @NonNull
        public String getMemo() { return memo; }
        @Nullable
        public PlantingDetail getPlanting() { return planting; }
        @Nullable
        public WateringDetail getWatering() { return watering; }
        @Nullable
        public FertilizingDetail getFertilizing() { return fertilizing; }
        @Nullable
        public PestControlDetail getPestControl() { return pestControl; }
        @Nullable
        public WeedingDetail getWeeding() { return weeding; }
        @Nullable
        public HarvestDetail getHarvest() { return harvest; }
        @NonNull
        public List<MediaItem> getImages() { return images; }
        @NonNull
        public LocalDateTime getCreatedAt() { return createdAt; }
        @NonNull
        public LocalDateTime getUpdatedAt() { return updatedAt; }
    }

    public record MediaItem(
        @NonNull UUID mediaId,
        @NonNull String url
    ) {
        public MediaItem {
            Objects.requireNonNull(mediaId, "mediaId");
            Objects.requireNonNull(url, "url");
        }

        @NonNull
        public UUID getMediaId() { return mediaId; }
        @NonNull
        public String getUrl() { return url; }
    }

    public record PlantingDetail(
        @NonNull PlantingMethod plantingMethod,
        @Nullable BigDecimal seedAmount,
        @Nullable SeedAmountUnit seedAmountUnit,
        @Nullable Integer seedlingCount,
        @Nullable SeedlingUnit seedlingUnit,
        @Nullable PropagationMethod propagationMethod
    ) {
        public PlantingDetail {
            Objects.requireNonNull(plantingMethod, "plantingMethod");
        }

        @NonNull
        public PlantingMethod getPlantingMethod() { return plantingMethod; }
        @Nullable
        public BigDecimal getSeedAmount() { return seedAmount; }
        @Nullable
        public SeedAmountUnit getSeedAmountUnit() { return seedAmountUnit; }
        @Nullable
        public Integer getSeedlingCount() { return seedlingCount; }
        @Nullable
        public SeedlingUnit getSeedlingUnit() { return seedlingUnit; }
        @Nullable
        public PropagationMethod getPropagationMethod() { return propagationMethod; }
    }

    public record WateringDetail(
        @Nullable IrrigationAmount irrigationAmount,
        @Nullable IrrigationMethod irrigationMethod
    ) {
        @Nullable
        public IrrigationAmount getIrrigationAmount() { return irrigationAmount; }
        @Nullable
        public IrrigationMethod getIrrigationMethod() { return irrigationMethod; }
    }

    public record FertilizingDetail(
        @NonNull String materialName,
        @NonNull BigDecimal amount,
        @NonNull FertilizerAmountUnit amountUnit,
        @Nullable FertilizingMethod applicationMethod
    ) {
        public FertilizingDetail {
            Objects.requireNonNull(materialName, "materialName");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(amountUnit, "amountUnit");
        }

        @NonNull
        public String getMaterialName() { return materialName; }
        @NonNull
        public BigDecimal getAmount() { return amount; }
        @NonNull
        public FertilizerAmountUnit getAmountUnit() { return amountUnit; }
        @Nullable
        public FertilizingMethod getApplicationMethod() { return applicationMethod; }
    }

    public record PestControlDetail(
        @NonNull UUID pesticideId,
        @NonNull String pesticideName,
        @NonNull BigDecimal pesticideAmount,
        @NonNull PesticideAmountUnit pesticideAmountUnit,
        @NonNull BigDecimal totalSprayAmount,
        @NonNull SprayAmountUnit totalSprayAmountUnit,
        @Nullable UUID pestId,
        @Nullable String pestName
    ) {
        public PestControlDetail {
            Objects.requireNonNull(pesticideId, "pesticideId");
            Objects.requireNonNull(pesticideName, "pesticideName");
            Objects.requireNonNull(pesticideAmount, "pesticideAmount");
            Objects.requireNonNull(pesticideAmountUnit, "pesticideAmountUnit");
            Objects.requireNonNull(totalSprayAmount, "totalSprayAmount");
            Objects.requireNonNull(totalSprayAmountUnit, "totalSprayAmountUnit");
        }

        @NonNull
        public UUID getPesticideId() { return pesticideId; }
        @NonNull
        public String getPesticideName() { return pesticideName; }
        @NonNull
        public BigDecimal getPesticideAmount() { return pesticideAmount; }
        @NonNull
        public PesticideAmountUnit getPesticideAmountUnit() { return pesticideAmountUnit; }
        @NonNull
        public BigDecimal getTotalSprayAmount() { return totalSprayAmount; }
        @NonNull
        public SprayAmountUnit getTotalSprayAmountUnit() { return totalSprayAmountUnit; }
        @Nullable
        public UUID getPestId() { return pestId; }
        @Nullable
        public String getPestName() { return pestName; }
    }

    public record WeedingDetail(
        @Nullable WeedingMethod weedingMethod
    ) {
        @Nullable
        public WeedingMethod getWeedingMethod() { return weedingMethod; }
    }

    public record HarvestDetail(
        @Nullable BigDecimal harvestAmount,
        @Nullable CropUsePartCategory medicinalPart,
        @NonNull HarvestSource harvestSource,
        int growthPeriod,
        boolean isLastHarvest
    ) {
        public HarvestDetail {
            Objects.requireNonNull(harvestSource, "harvestSource");
        }

        @Nullable
        public BigDecimal getHarvestAmount() { return harvestAmount; }
        @Nullable
        public CropUsePartCategory getMedicinalPart() { return medicinalPart; }
        @NonNull
        public HarvestSource getHarvestSource() { return harvestSource; }
        public int getGrowthPeriod() { return growthPeriod; }
        @com.fasterxml.jackson.annotation.JsonProperty("isLastHarvest")
        public boolean isLastHarvest() { return isLastHarvest; }
    }
}
