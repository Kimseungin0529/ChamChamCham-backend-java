package com.chamchamcham.domain.farming;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

public record FarmingWorkReportSourceSnapshot(
    List<FarmingRecord> records,
    Set<UUID> finalHarvestRecordIds,
    Map<UUID, String> firstImageUrlByRecordId
) {
    public FarmingWorkReportSourceSnapshot {
        Objects.requireNonNull(records, "records");
        Objects.requireNonNull(finalHarvestRecordIds, "finalHarvestRecordIds");
        Objects.requireNonNull(firstImageUrlByRecordId, "firstImageUrlByRecordId");
    }

}
