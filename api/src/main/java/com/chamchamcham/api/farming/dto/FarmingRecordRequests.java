package com.chamchamcham.api.farming.dto;

import com.chamchamcham.application.farming.FarmingRecordCommand;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class FarmingRecordRequests {
    private FarmingRecordRequests() {}

    public record SaveRecordRequest(
        @NotNull(message = "농지를 선택해주세요")
        @Nullable UUID farmId,
        @NotNull(message = "작물을 선택해주세요")
        @Nullable UUID cropId,
        @NotNull(message = "작업 유형을 선택해주세요")
        @Nullable WorkType workType,
        @NotNull(message = "작업 일시를 입력해주세요")
        @Nullable LocalDateTime workedAt,
        @NotBlank(message = "날씨 상태를 입력해주세요")
        @NonNull String weatherCondition,
        @NotNull(message = "기온을 입력해주세요")
        @Nullable Integer weatherTemperature,
        @NotBlank(message = "메모를 입력해주세요")
        @Size(min = 30, max = 500, message = "메모는 30자 이상 500자 이내로 입력해주세요")
        @NonNull String memo,
        @Valid
        @Nullable PlantingDetailRequest planting,
        @Valid
        @Nullable WateringDetailRequest watering,
        @Valid
        @Nullable FertilizingDetailRequest fertilizing,
        @Valid
        @Nullable PestControlDetailRequest pestControl,
        @Valid
        @Nullable WeedingDetailRequest weeding,
        @Valid
        @Nullable HarvestDetailRequest harvest,
        @Size(max = 5, message = "사진은 최대 5장까지 첨부할 수 있습니다")
        @UniqueElements(message = "중복된 사진은 첨부할 수 없습니다")
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = MediaIdsDefault.class)
        @NonNull List<UUID> mediaIds,
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = EntryModeDefault.class)
        @NonNull EntryMode entryMode
    ) {
        public SaveRecordRequest {
            Objects.requireNonNull(weatherCondition, "weatherCondition");
            Objects.requireNonNull(memo, "memo");
            Objects.requireNonNull(mediaIds, "mediaIds");
            Objects.requireNonNull(entryMode, "entryMode");
        }
        @Nullable public UUID getFarmId() { return farmId; }
        @Nullable public UUID getCropId() { return cropId; }
        @Nullable public WorkType getWorkType() { return workType; }
        @Nullable public LocalDateTime getWorkedAt() { return workedAt; }
        @NonNull public String getWeatherCondition() { return weatherCondition; }
        @Nullable public Integer getWeatherTemperature() { return weatherTemperature; }
        @NonNull public String getMemo() { return memo; }
        @Nullable public PlantingDetailRequest getPlanting() { return planting; }
        @Nullable public WateringDetailRequest getWatering() { return watering; }
        @Nullable public FertilizingDetailRequest getFertilizing() { return fertilizing; }
        @Nullable public PestControlDetailRequest getPestControl() { return pestControl; }
        @Nullable public WeedingDetailRequest getWeeding() { return weeding; }
        @Nullable public HarvestDetailRequest getHarvest() { return harvest; }
        @NonNull public List<UUID> getMediaIds() { return mediaIds; }
        @NonNull public EntryMode getEntryMode() { return entryMode; }
        public FarmingRecordCommand.Create toCreateCommand(UUID memberId) { return toCreateCommand(memberId, entryMode); }
        public FarmingRecordCommand.Create toCreateCommand(UUID memberId, EntryMode mode) {
            return new FarmingRecordCommand.Create(memberId,
                Objects.requireNonNull(farmId),
                Objects.requireNonNull(cropId),
                Objects.requireNonNull(workType),
                Objects.requireNonNull(workedAt),
                weatherCondition,
                Objects.requireNonNull(weatherTemperature),
                memo,
                planting == null ? null : planting.toCommand(),
                watering == null ? null : watering.toCommand(),
                fertilizing == null ? null : fertilizing.toCommand(),
                pestControl == null ? null : pestControl.toCommand(),
                weeding == null ? null : weeding.toCommand(),
                harvest == null ? null : harvest.toCommand(),
                mediaIds,
                mode);
        }
        public FarmingRecordCommand.Update toUpdateCommand(UUID memberId, UUID recordId) {
            return new FarmingRecordCommand.Update(memberId,
                recordId,
                Objects.requireNonNull(farmId),
                Objects.requireNonNull(cropId),
                Objects.requireNonNull(workType),
                Objects.requireNonNull(workedAt),
                weatherCondition,
                Objects.requireNonNull(weatherTemperature),
                memo,
                planting == null ? null : planting.toCommand(),
                watering == null ? null : watering.toCommand(),
                fertilizing == null ? null : fertilizing.toCommand(),
                pestControl == null ? null : pestControl.toCommand(),
                weeding == null ? null : weeding.toCommand(),
                harvest == null ? null : harvest.toCommand(),
                mediaIds);
        }
    }

    public record PlantingDetailRequest(
        @NotNull(message = "심기 방법을 선택해주세요")
        @Nullable PlantingMethod plantingMethod,
        @DecimalMin(value = "0.01", message = "파종량은 0보다 커야 합니다")
        @Nullable BigDecimal seedAmount,
        @Nullable SeedAmountUnit seedAmountUnit,
        @Min(value = 1, message = "모종수는 1 이상이어야 합니다")
        @Nullable Integer seedlingCount,
        @Nullable SeedlingUnit seedlingUnit,
        @Nullable PropagationMethod propagationMethod
    ) {
        @Nullable public PlantingMethod getPlantingMethod() { return plantingMethod; }
        @Nullable public BigDecimal getSeedAmount() { return seedAmount; }
        @Nullable public SeedAmountUnit getSeedAmountUnit() { return seedAmountUnit; }
        @Nullable public Integer getSeedlingCount() { return seedlingCount; }
        @Nullable public SeedlingUnit getSeedlingUnit() { return seedlingUnit; }
        @Nullable public PropagationMethod getPropagationMethod() { return propagationMethod; }
        public FarmingRecordCommand.PlantingDetail toCommand() { return new FarmingRecordCommand.PlantingDetail(Objects.requireNonNull(plantingMethod),
                seedAmount,
                seedAmountUnit,
                seedlingCount,
                seedlingUnit,
                propagationMethod); }
    }

    public record WateringDetailRequest(
        @Nullable IrrigationAmount irrigationAmount,
        @Nullable IrrigationMethod irrigationMethod
    ) {
        @Nullable public IrrigationAmount getIrrigationAmount() { return irrigationAmount; }
        @Nullable public IrrigationMethod getIrrigationMethod() { return irrigationMethod; }
        public FarmingRecordCommand.WateringDetail toCommand() { return new FarmingRecordCommand.WateringDetail(irrigationAmount, irrigationMethod); }
    }

    public record FertilizingDetailRequest(
        @NotBlank(message = "자재명을 입력해주세요")
        @NonNull String materialName,
        @DecimalMin(value = "0.01", message = "시비량은 0보다 커야 합니다")
        @NonNull BigDecimal amount,
        @NonNull FertilizerAmountUnit amountUnit,
        @Nullable FertilizingMethod applicationMethod
    ) {
        public FertilizingDetailRequest {
            Objects.requireNonNull(materialName, "materialName");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(amountUnit, "amountUnit");
        }
        @NonNull public String getMaterialName() { return materialName; }
        @NonNull public BigDecimal getAmount() { return amount; }
        @NonNull public FertilizerAmountUnit getAmountUnit() { return amountUnit; }
        @Nullable public FertilizingMethod getApplicationMethod() { return applicationMethod; }
        public FarmingRecordCommand.FertilizingDetail toCommand() { return new FarmingRecordCommand.FertilizingDetail(materialName,
                amount,
                amountUnit,
                applicationMethod); }
    }

    public record PestControlDetailRequest(
        @NotNull(message = "농약을 선택해주세요")
        @Nullable UUID pesticideId,
        @DecimalMin(value = "0.01", message = "농약량은 0보다 커야 합니다")
        @NonNull BigDecimal pesticideAmount,
        @NonNull PesticideAmountUnit pesticideAmountUnit,
        @DecimalMin(value = "0.01", message = "살포량은 0보다 커야 합니다")
        @NonNull BigDecimal totalSprayAmount,
        @NonNull SprayAmountUnit totalSprayAmountUnit,
        @Nullable UUID pestId
    ) {
        public PestControlDetailRequest {
            Objects.requireNonNull(pesticideAmount, "pesticideAmount");
            Objects.requireNonNull(pesticideAmountUnit, "pesticideAmountUnit");
            Objects.requireNonNull(totalSprayAmount, "totalSprayAmount");
            Objects.requireNonNull(totalSprayAmountUnit, "totalSprayAmountUnit");
        }
        @Nullable public UUID getPesticideId() { return pesticideId; }
        @NonNull public BigDecimal getPesticideAmount() { return pesticideAmount; }
        @NonNull public PesticideAmountUnit getPesticideAmountUnit() { return pesticideAmountUnit; }
        @NonNull public BigDecimal getTotalSprayAmount() { return totalSprayAmount; }
        @NonNull public SprayAmountUnit getTotalSprayAmountUnit() { return totalSprayAmountUnit; }
        @Nullable public UUID getPestId() { return pestId; }
        public FarmingRecordCommand.PestControlDetail toCommand() { return new FarmingRecordCommand.PestControlDetail(Objects.requireNonNull(pesticideId),
                pesticideAmount,
                pesticideAmountUnit,
                totalSprayAmount,
                totalSprayAmountUnit,
                pestId); }
    }

    public record WeedingDetailRequest(
        @Nullable WeedingMethod weedingMethod
    ) {
        @Nullable public WeedingMethod getWeedingMethod() { return weedingMethod; }
        public FarmingRecordCommand.WeedingDetail toCommand() { return new FarmingRecordCommand.WeedingDetail(weedingMethod); }
    }

    public record HarvestDetailRequest(
        @DecimalMin(value = "0.01", message = "수확량은 0보다 커야 합니다")
        @Nullable BigDecimal harvestAmount,
        boolean harvestAmountUnknown,
        @Nullable CropUsePartCategory medicinalPart,
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = HarvestSourceDefault.class)
        @NonNull HarvestSource harvestSource,
        @NotNull(message = "재배기간을 입력해주세요")
        @Min(value = 1, message = "재배기간은 1 이상이어야 합니다")
        @Nullable Integer growthPeriod,
        @NotNull(message = "마지막 수확 여부를 선택해주세요")
        @Nullable Boolean isLastHarvest
    ) {
        public HarvestDetailRequest {
            Objects.requireNonNull(harvestSource, "harvestSource");
        }
        @Nullable public BigDecimal getHarvestAmount() { return harvestAmount; }
        public boolean getHarvestAmountUnknown() { return harvestAmountUnknown; }
        @Nullable public CropUsePartCategory getMedicinalPart() { return medicinalPart; }
        @NonNull public HarvestSource getHarvestSource() { return harvestSource; }
        @Nullable public Integer getGrowthPeriod() { return growthPeriod; }
        @JsonProperty("isLastHarvest")
        @Nullable public Boolean getIsLastHarvest() { return isLastHarvest; }
        public FarmingRecordCommand.HarvestDetail toCommand() { return new FarmingRecordCommand.HarvestDetail(harvestAmount,
                harvestAmountUnknown,
                medicinalPart,
                harvestSource,
                Objects.requireNonNull(growthPeriod),
                Objects.requireNonNull(isLastHarvest)); }
    }

    public static final class MediaIdsDefault extends com.fasterxml.jackson.databind.JsonDeserializer<List<UUID>> {
        @Override public List<UUID> deserialize(com.fasterxml.jackson.core.JsonParser parser,
            com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, context.getTypeFactory().constructCollectionType(List.class, UUID.class));
        }
        @Override public List<UUID> getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return java.util.Collections.emptyList(); }
    }

    public static final class EntryModeDefault extends com.fasterxml.jackson.databind.JsonDeserializer<EntryMode> {
        @Override public EntryMode deserialize(com.fasterxml.jackson.core.JsonParser parser,
            com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, EntryMode.class);
        }
        @Override public EntryMode getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return EntryMode.MANUAL; }
    }

    public static final class HarvestSourceDefault extends com.fasterxml.jackson.databind.JsonDeserializer<HarvestSource> {
        @Override public HarvestSource deserialize(com.fasterxml.jackson.core.JsonParser parser,
            com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, HarvestSource.class);
        }
        @Override public HarvestSource getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return HarvestSource.CULTIVATED; }
    }
}
