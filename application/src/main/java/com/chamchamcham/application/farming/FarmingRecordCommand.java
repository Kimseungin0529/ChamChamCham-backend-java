package com.chamchamcham.application.farming;

import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.EntryMode;
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
import java.util.Set;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class FarmingRecordCommand {
    private FarmingRecordCommand() {}

    public record Create(
        @NonNull UUID memberId,
        @NonNull UUID farmId,
        @NonNull UUID cropId,
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
        @NonNull List<UUID> mediaIds,
        @NonNull EntryMode entryMode
    ) implements FarmingRecordDetailPayload {
        public Create {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(workType, "workType");
            Objects.requireNonNull(workedAt, "workedAt");
            Objects.requireNonNull(weatherCondition, "weatherCondition");
            Objects.requireNonNull(memo, "memo");
            Objects.requireNonNull(mediaIds, "mediaIds");
            Objects.requireNonNull(entryMode, "entryMode");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public UUID getFarmId() { return farmId; }
        @NonNull
        public UUID getCropId() { return cropId; }
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
        public List<UUID> getMediaIds() { return mediaIds; }
        @NonNull
        public EntryMode getEntryMode() { return entryMode; }
    }

    public record Update(
        @NonNull UUID memberId,
        @NonNull UUID recordId,
        @NonNull UUID farmId,
        @NonNull UUID cropId,
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
        @NonNull List<UUID> mediaIds
    ) implements FarmingRecordDetailPayload {
        public Update {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(recordId, "recordId");
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(workType, "workType");
            Objects.requireNonNull(workedAt, "workedAt");
            Objects.requireNonNull(weatherCondition, "weatherCondition");
            Objects.requireNonNull(memo, "memo");
            Objects.requireNonNull(mediaIds, "mediaIds");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public UUID getRecordId() { return recordId; }
        @NonNull
        public UUID getFarmId() { return farmId; }
        @NonNull
        public UUID getCropId() { return cropId; }
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
        public List<UUID> getMediaIds() { return mediaIds; }
    }

    public record Delete(
        @NonNull UUID memberId,
        @NonNull UUID recordId
    ) {
        public Delete {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(recordId, "recordId");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public UUID getRecordId() { return recordId; }
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
        @NonNull BigDecimal pesticideAmount,
        @NonNull PesticideAmountUnit pesticideAmountUnit,
        @NonNull BigDecimal totalSprayAmount,
        @NonNull SprayAmountUnit totalSprayAmountUnit,
        @Nullable UUID pestId
    ) {
        public PestControlDetail {
            Objects.requireNonNull(pesticideId, "pesticideId");
            Objects.requireNonNull(pesticideAmount, "pesticideAmount");
            Objects.requireNonNull(pesticideAmountUnit, "pesticideAmountUnit");
            Objects.requireNonNull(totalSprayAmount, "totalSprayAmount");
            Objects.requireNonNull(totalSprayAmountUnit, "totalSprayAmountUnit");
        }

        @NonNull
        public UUID getPesticideId() { return pesticideId; }
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
    }

    public record WeedingDetail(
        @Nullable WeedingMethod weedingMethod
    ) {
        @Nullable
        public WeedingMethod getWeedingMethod() { return weedingMethod; }
    }

    public record HarvestDetail(
        @Nullable BigDecimal harvestAmount,
        boolean amountUnknown,
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
        public boolean getAmountUnknown() { return amountUnknown; }
        @Nullable
        public CropUsePartCategory getMedicinalPart() { return medicinalPart; }
        @NonNull
        public HarvestSource getHarvestSource() { return harvestSource; }
        public int getGrowthPeriod() { return growthPeriod; }
        public boolean isLastHarvest() { return isLastHarvest; }
    }
}
