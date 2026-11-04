package com.chamchamcham.application.farming;

import com.chamchamcham.domain.farming.WorkType;
import org.springframework.lang.Nullable;

public interface FarmingRecordDetailPayload {
    WorkType getWorkType();
    @Nullable FarmingRecordCommand.PlantingDetail getPlanting();
    @Nullable FarmingRecordCommand.WateringDetail getWatering();
    @Nullable FarmingRecordCommand.FertilizingDetail getFertilizing();
    @Nullable FarmingRecordCommand.PestControlDetail getPestControl();
    @Nullable FarmingRecordCommand.WeedingDetail getWeeding();
    @Nullable FarmingRecordCommand.HarvestDetail getHarvest();
}
