package com.chamchamcham.domain.report;

import java.util.Objects;
import java.time.LocalDateTime;
import com.chamchamcham.domain.farming.FarmingRecord;

public record FarmingCycleReportProjection(
    FarmingCycleReportStatus status,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    FarmingCycleStartBasis startBasis,
    FarmingRecord finalHarvestRecord,
    int statisticsSchemaVersion,
    CycleReportStatistics statistics
) {
    public FarmingCycleReportProjection {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(startsAt, "startsAt");
        Objects.requireNonNull(startBasis, "startBasis");
        Objects.requireNonNull(statistics, "statistics");
    }

}
