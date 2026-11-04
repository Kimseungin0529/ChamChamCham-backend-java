package com.chamchamcham.application.voice;

import com.chamchamcham.application.farming.FarmingRecordCommand;
import com.chamchamcham.domain.farming.WorkType;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.lang.Nullable;

public record VoiceRecordCandidate(
    @Nullable UUID farmId,
    @Nullable UUID cropId,
    @Nullable WorkType workType,
    @Nullable LocalDateTime workedAt,
    @Nullable String weatherCondition,
    @Nullable Integer weatherTemperature,
    @Nullable String memo,
    @Nullable FarmingRecordCommand.PlantingDetail planting,
    @Nullable FarmingRecordCommand.WateringDetail watering,
    @Nullable FarmingRecordCommand.FertilizingDetail fertilizing,
    @Nullable FarmingRecordCommand.PestControlDetail pestControl,
    @Nullable FarmingRecordCommand.WeedingDetail weeding,
    @Nullable FarmingRecordCommand.HarvestDetail harvest
) {
    @Nullable
    public UUID getFarmId() { return farmId; }
    @Nullable
    public UUID getCropId() { return cropId; }
    @Nullable
    public WorkType getWorkType() { return workType; }
    @Nullable
    public LocalDateTime getWorkedAt() { return workedAt; }
    @Nullable
    public String getWeatherCondition() { return weatherCondition; }
    @Nullable
    public Integer getWeatherTemperature() { return weatherTemperature; }
    @Nullable
    public String getMemo() { return memo; }
    @Nullable
    public FarmingRecordCommand.PlantingDetail getPlanting() { return planting; }
    @Nullable
    public FarmingRecordCommand.WateringDetail getWatering() { return watering; }
    @Nullable
    public FarmingRecordCommand.FertilizingDetail getFertilizing() { return fertilizing; }
    @Nullable
    public FarmingRecordCommand.PestControlDetail getPestControl() { return pestControl; }
    @Nullable
    public FarmingRecordCommand.WeedingDetail getWeeding() { return weeding; }
    @Nullable
    public FarmingRecordCommand.HarvestDetail getHarvest() { return harvest; }
    public VoiceRecordCandidate() { this(null, null, null, null, null, null, null, null, null, null, null, null, null); }
}
