package com.chamchamcham.application.report;

import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class CycleSlice {
    private final FarmingCycleReportStatus status;
    private final FarmingCycleStartBasis startBasis;
    private final List<CycleReportSourceRecord> records;
    private final UUID finalHarvestRecordId;

    @JsonCreator
    public CycleSlice(@JsonProperty("status") @NonNull FarmingCycleReportStatus status,
            @JsonProperty("startBasis") @NonNull FarmingCycleStartBasis startBasis,
            @JsonProperty("records") @NonNull List<CycleReportSourceRecord> records) {
        this.status = Objects.requireNonNull(status, "status");
        this.startBasis = Objects.requireNonNull(startBasis, "startBasis");
        this.records = Objects.requireNonNull(records, "records");
        var last = records.isEmpty() ? null : records.getLast();
        this.finalHarvestRecordId = last != null && last.getHarvest() != null && last.getHarvest().isLastHarvest() ? last.getId() : null;
    }

    @NonNull public FarmingCycleReportStatus getStatus() { return status; }
    @NonNull public FarmingCycleStartBasis getStartBasis() { return startBasis; }
    @NonNull public List<CycleReportSourceRecord> getRecords() { return records; }
    @Nullable
    public UUID getFinalHarvestRecordId() { return finalHarvestRecordId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof CycleSlice that && status == that.status && startBasis == that.startBasis && records.equals(that.records);
    }

    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result + startBasis.hashCode();
        return 31 * result + records.hashCode();
    }

    @Override
    public String toString() { return "CycleSlice(status=" + status + ", startBasis=" + startBasis + ", records=" + records + ")"; }
}
