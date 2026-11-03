package com.chamchamcham.domain.farming;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

public record FarmingCycleReportSourceSnapshot(
    List<FarmingRecord> records,
    Map<UUID, PlantingRecord> plantingByRecordId,
    Map<UUID, WateringRecord> wateringByRecordId,
    Map<UUID, FertilizingRecord> fertilizingByRecordId,
    Map<UUID, PestControlRecord> pestControlByRecordId,
    Map<UUID, WeedingRecord> weedingByRecordId,
    Map<UUID, HarvestRecord> harvestByRecordId,
    Set<UUID> mediaRecordIds
) {
    public FarmingCycleReportSourceSnapshot {
        Objects.requireNonNull(records, "records");
        Objects.requireNonNull(plantingByRecordId, "plantingByRecordId");
        Objects.requireNonNull(wateringByRecordId, "wateringByRecordId");
        Objects.requireNonNull(fertilizingByRecordId, "fertilizingByRecordId");
        Objects.requireNonNull(pestControlByRecordId, "pestControlByRecordId");
        Objects.requireNonNull(weedingByRecordId, "weedingByRecordId");
        Objects.requireNonNull(harvestByRecordId, "harvestByRecordId");
        Objects.requireNonNull(mediaRecordIds, "mediaRecordIds");
    }

}
