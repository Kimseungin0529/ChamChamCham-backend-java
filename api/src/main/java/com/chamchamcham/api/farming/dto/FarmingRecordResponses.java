package com.chamchamcham.api.farming.dto;

import com.chamchamcham.application.farming.FarmingRecordResult;
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
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class FarmingRecordResponses {
    private FarmingRecordResponses() {}

    public record RecordIdResponse(
        @NonNull UUID id,
        @NonNull WorkType workType
    ) {
        public RecordIdResponse {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(workType, "workType");
        }
        @NonNull public UUID getId() { return id; }
        @NonNull public WorkType getWorkType() { return workType; }
        public static RecordIdResponse from(FarmingRecordResult.RecordId result) {
            return new RecordIdResponse(
                result.getId(),
                result.getWorkType()
            );
        }
    }

    public record RecordSummaryResponse(
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
        public RecordSummaryResponse {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
            Objects.requireNonNull(workType, "workType");
            Objects.requireNonNull(workedAt, "workedAt");
            Objects.requireNonNull(weatherCondition, "weatherCondition");
            Objects.requireNonNull(memoPreview, "memoPreview");
        }
        @NonNull public UUID getId() { return id; }
        @NonNull public UUID getCropId() { return cropId; }
        @NonNull public String getCropName() { return cropName; }
        @NonNull public WorkType getWorkType() { return workType; }
        @NonNull public LocalDateTime getWorkedAt() { return workedAt; }
        @NonNull public String getWeatherCondition() { return weatherCondition; }
        public int getWeatherTemperature() { return weatherTemperature; }
        @NonNull public String getMemoPreview() { return memoPreview; }
        @Nullable public String getThumbnailUrl() { return thumbnailUrl; }
        @Nullable public IrrigationMethod getIrrigationMethod() { return irrigationMethod; }
        @Nullable public BigDecimal getHarvestAmount() { return harvestAmount; }
        @Nullable public String getPesticideName() { return pesticideName; }
        @Nullable public WeedingMethod getWeedingMethod() { return weedingMethod; }
        @Nullable public PlantingMethod getPlantingMethod() { return plantingMethod; }
        @Nullable public String getMaterialName() { return materialName; }
        public static RecordSummaryResponse from(FarmingRecordResult.Summary result) {
            return new RecordSummaryResponse(
                result.getId(),
                result.getCropId(),
                result.getCropName(),
                result.getWorkType(),
                result.getWorkedAt(),
                result.getWeatherCondition(),
                result.getWeatherTemperature(),
                result.getMemoPreview(),
                result.getThumbnailUrl(),
                result.getIrrigationMethod(),
                result.getHarvestAmount(),
                result.getPesticideName(),
                result.getWeedingMethod(),
                result.getPlantingMethod(),
                result.getMaterialName()
            );
        }
    }

    public record RecordPageResponse(
        @NonNull List<RecordSummaryResponse> items,
        @Nullable String nextCursor
    ) {
        public RecordPageResponse {
            Objects.requireNonNull(items, "items");
        }
        @NonNull public List<RecordSummaryResponse> getItems() { return items; }
        @Nullable public String getNextCursor() { return nextCursor; }
        public static RecordPageResponse from(FarmingRecordResult.Page result) {
            return new RecordPageResponse(
                result.getItems().stream().map(RecordSummaryResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                result.getNextCursor()
            );
        }
    }

    public record RecordDetailResponse(
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
        @Nullable PlantingDetailResponse planting,
        @Nullable WateringDetailResponse watering,
        @Nullable FertilizingDetailResponse fertilizing,
        @Nullable PestControlDetailResponse pestControl,
        @Nullable WeedingDetailResponse weeding,
        @Nullable HarvestDetailResponse harvest,
        @NonNull List<MediaResponse> images,
        @NonNull LocalDateTime createdAt,
        @NonNull LocalDateTime updatedAt
    ) {
        public RecordDetailResponse {
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
        @NonNull public UUID getId() { return id; }
        @NonNull public UUID getFarmId() { return farmId; }
        @NonNull public String getFarmName() { return farmName; }
        @NonNull public UUID getCropId() { return cropId; }
        @NonNull public String getCropName() { return cropName; }
        @NonNull public WorkType getWorkType() { return workType; }
        @NonNull public LocalDateTime getWorkedAt() { return workedAt; }
        @NonNull public String getWeatherCondition() { return weatherCondition; }
        public int getWeatherTemperature() { return weatherTemperature; }
        @NonNull public String getMemo() { return memo; }
        @Nullable public PlantingDetailResponse getPlanting() { return planting; }
        @Nullable public WateringDetailResponse getWatering() { return watering; }
        @Nullable public FertilizingDetailResponse getFertilizing() { return fertilizing; }
        @Nullable public PestControlDetailResponse getPestControl() { return pestControl; }
        @Nullable public WeedingDetailResponse getWeeding() { return weeding; }
        @Nullable public HarvestDetailResponse getHarvest() { return harvest; }
        @NonNull public List<MediaResponse> getImages() { return images; }
        @NonNull public LocalDateTime getCreatedAt() { return createdAt; }
        @NonNull public LocalDateTime getUpdatedAt() { return updatedAt; }
        public static RecordDetailResponse from(FarmingRecordResult.Detail result) {
            return new RecordDetailResponse(
                result.getId(),
                result.getFarmId(),
                result.getFarmName(),
                result.getCropId(),
                result.getCropName(),
                result.getWorkType(),
                result.getWorkedAt(),
                result.getWeatherCondition(),
                result.getWeatherTemperature(),
                result.getMemo(),
                (result.getPlanting() == null ? null : PlantingDetailResponse.from(result.getPlanting())),
                (result.getWatering() == null ? null : WateringDetailResponse.from(result.getWatering())),
                (result.getFertilizing() == null ? null : FertilizingDetailResponse.from(result.getFertilizing())),
                (result.getPestControl() == null ? null : PestControlDetailResponse.from(result.getPestControl())),
                (result.getWeeding() == null ? null : WeedingDetailResponse.from(result.getWeeding())),
                (result.getHarvest() == null ? null : HarvestDetailResponse.from(result.getHarvest())),
                result.getImages().stream().map(MediaResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                result.getCreatedAt(),
                result.getUpdatedAt()
            );
        }
    }

    public record MediaResponse(
        @NonNull UUID mediaId,
        @NonNull String url
    ) {
        public MediaResponse {
            Objects.requireNonNull(mediaId, "mediaId");
            Objects.requireNonNull(url, "url");
        }
        @NonNull public UUID getMediaId() { return mediaId; }
        @NonNull public String getUrl() { return url; }
        public static MediaResponse from(FarmingRecordResult.MediaItem result) {
            return new MediaResponse(
                result.getMediaId(),
                result.getUrl()
            );
        }
    }

    public record PlantingDetailResponse(
        @NonNull PlantingMethod plantingMethod,
        @Nullable BigDecimal seedAmount,
        @Nullable SeedAmountUnit seedAmountUnit,
        @Nullable Integer seedlingCount,
        @Nullable SeedlingUnit seedlingUnit,
        @Nullable PropagationMethod propagationMethod
    ) {
        public PlantingDetailResponse {
            Objects.requireNonNull(plantingMethod, "plantingMethod");
        }
        @NonNull public PlantingMethod getPlantingMethod() { return plantingMethod; }
        @Nullable public BigDecimal getSeedAmount() { return seedAmount; }
        @Nullable public SeedAmountUnit getSeedAmountUnit() { return seedAmountUnit; }
        @Nullable public Integer getSeedlingCount() { return seedlingCount; }
        @Nullable public SeedlingUnit getSeedlingUnit() { return seedlingUnit; }
        @Nullable public PropagationMethod getPropagationMethod() { return propagationMethod; }
        public static PlantingDetailResponse from(FarmingRecordResult.PlantingDetail result) {
            return new PlantingDetailResponse(
                result.getPlantingMethod(),
                result.getSeedAmount(),
                result.getSeedAmountUnit(),
                result.getSeedlingCount(),
                result.getSeedlingUnit(),
                result.getPropagationMethod()
            );
        }
    }

    public record WateringDetailResponse(
        @Nullable IrrigationAmount irrigationAmount,
        @Nullable IrrigationMethod irrigationMethod
    ) {
        @Nullable public IrrigationAmount getIrrigationAmount() { return irrigationAmount; }
        @Nullable public IrrigationMethod getIrrigationMethod() { return irrigationMethod; }
        public static WateringDetailResponse from(FarmingRecordResult.WateringDetail result) {
            return new WateringDetailResponse(
                result.getIrrigationAmount(),
                result.getIrrigationMethod()
            );
        }
    }

    public record FertilizingDetailResponse(
        @NonNull String materialName,
        @NonNull BigDecimal amount,
        @NonNull FertilizerAmountUnit amountUnit,
        @Nullable FertilizingMethod applicationMethod
    ) {
        public FertilizingDetailResponse {
            Objects.requireNonNull(materialName, "materialName");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(amountUnit, "amountUnit");
        }
        @NonNull public String getMaterialName() { return materialName; }
        @NonNull public BigDecimal getAmount() { return amount; }
        @NonNull public FertilizerAmountUnit getAmountUnit() { return amountUnit; }
        @Nullable public FertilizingMethod getApplicationMethod() { return applicationMethod; }
        public static FertilizingDetailResponse from(FarmingRecordResult.FertilizingDetail result) {
            return new FertilizingDetailResponse(
                result.getMaterialName(),
                result.getAmount(),
                result.getAmountUnit(),
                result.getApplicationMethod()
            );
        }
    }

    public record PestControlDetailResponse(
        @NonNull UUID pesticideId,
        @NonNull String pesticideName,
        @NonNull BigDecimal pesticideAmount,
        @NonNull PesticideAmountUnit pesticideAmountUnit,
        @NonNull BigDecimal totalSprayAmount,
        @NonNull SprayAmountUnit totalSprayAmountUnit,
        @Nullable UUID pestId,
        @Nullable String pestName
    ) {
        public PestControlDetailResponse {
            Objects.requireNonNull(pesticideId, "pesticideId");
            Objects.requireNonNull(pesticideName, "pesticideName");
            Objects.requireNonNull(pesticideAmount, "pesticideAmount");
            Objects.requireNonNull(pesticideAmountUnit, "pesticideAmountUnit");
            Objects.requireNonNull(totalSprayAmount, "totalSprayAmount");
            Objects.requireNonNull(totalSprayAmountUnit, "totalSprayAmountUnit");
        }
        @NonNull public UUID getPesticideId() { return pesticideId; }
        @NonNull public String getPesticideName() { return pesticideName; }
        @NonNull public BigDecimal getPesticideAmount() { return pesticideAmount; }
        @NonNull public PesticideAmountUnit getPesticideAmountUnit() { return pesticideAmountUnit; }
        @NonNull public BigDecimal getTotalSprayAmount() { return totalSprayAmount; }
        @NonNull public SprayAmountUnit getTotalSprayAmountUnit() { return totalSprayAmountUnit; }
        @Nullable public UUID getPestId() { return pestId; }
        @Nullable public String getPestName() { return pestName; }
        public static PestControlDetailResponse from(FarmingRecordResult.PestControlDetail result) {
            return new PestControlDetailResponse(
                result.getPesticideId(),
                result.getPesticideName(),
                result.getPesticideAmount(),
                result.getPesticideAmountUnit(),
                result.getTotalSprayAmount(),
                result.getTotalSprayAmountUnit(),
                result.getPestId(),
                result.getPestName()
            );
        }
    }

    public record WeedingDetailResponse(
        @Nullable WeedingMethod weedingMethod
    ) {
        @Nullable public WeedingMethod getWeedingMethod() { return weedingMethod; }
        public static WeedingDetailResponse from(FarmingRecordResult.WeedingDetail result) {
            return new WeedingDetailResponse(
                result.getWeedingMethod()
            );
        }
    }

    public record HarvestDetailResponse(
        @Nullable BigDecimal harvestAmount,
        boolean amountUnknown,
        @Nullable CropUsePartCategory medicinalPart,
        @NonNull HarvestSource harvestSource,
        int growthPeriod,
        boolean isLastHarvest
    ) {
        public HarvestDetailResponse {
            Objects.requireNonNull(harvestSource, "harvestSource");
        }
        @Nullable public BigDecimal getHarvestAmount() { return harvestAmount; }
        public boolean getAmountUnknown() { return amountUnknown; }
        @Nullable public CropUsePartCategory getMedicinalPart() { return medicinalPart; }
        @NonNull public HarvestSource getHarvestSource() { return harvestSource; }
        public int getGrowthPeriod() { return growthPeriod; }
        @JsonProperty("isLastHarvest")
        public boolean isLastHarvest() { return isLastHarvest; }
        public static HarvestDetailResponse from(FarmingRecordResult.HarvestDetail result) {
            return new HarvestDetailResponse(
                result.getHarvestAmount(),
                result.getHarvestAmount() == null,
                result.getMedicinalPart(),
                result.getHarvestSource(),
                result.getGrowthPeriod(),
                result.isLastHarvest()
            );
        }
    }
}
