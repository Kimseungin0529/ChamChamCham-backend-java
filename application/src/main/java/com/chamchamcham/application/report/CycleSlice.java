package com.chamchamcham.application.report;

import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record CycleSlice(
    @NonNull FarmingCycleReportStatus status,
    @NonNull FarmingCycleStartBasis startBasis,
    @NonNull List<CycleReportSourceRecord> records
) {
    public CycleSlice {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(startBasis, "startBasis");
        Objects.requireNonNull(records, "records");
    }

    @NonNull
    public FarmingCycleReportStatus getStatus() { return status; }
    @NonNull
    public FarmingCycleStartBasis getStartBasis() { return startBasis; }
    @NonNull
    public List<CycleReportSourceRecord> getRecords() { return records; }
    @Nullable
    public UUID getFinalHarvestRecordId() {
        if (records.isEmpty()) return null;
        var last = records.getLast();
        return last.getHarvest() != null && last.getHarvest().isLastHarvest() ? last.getId() : null;
    }
}
