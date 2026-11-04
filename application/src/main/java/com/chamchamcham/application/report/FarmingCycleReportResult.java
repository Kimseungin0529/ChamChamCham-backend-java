package com.chamchamcham.application.report;

import com.chamchamcham.domain.report.CycleReportStatistics;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class FarmingCycleReportResult {
    private FarmingCycleReportResult() {}

    public record Metadata(
        @NonNull UUID id,
        @NonNull UUID farmId,
        @NonNull String farmName,
        @NonNull UUID cropId,
        @NonNull String cropName,
        @NonNull FarmingCycleReportStatus status,
        @NonNull LocalDateTime startsAt,
        @Nullable LocalDateTime endsAt,
        @NonNull FarmingCycleStartBasis startBasis,
        long sourceRevision
    ) {
        public Metadata {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(farmName, "farmName");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(startsAt, "startsAt");
            Objects.requireNonNull(startBasis, "startBasis");
        }

        @NonNull
        public UUID getId() { return id; }
        @NonNull
        public UUID getFarmId() { return farmId; }
        @NonNull
        public String getFarmName() { return farmName; }
        @NonNull
        public UUID getCropId() { return cropId; }
        @NonNull
        public String getCropName() { return cropName; }
        @NonNull
        public FarmingCycleReportStatus getStatus() { return status; }
        @NonNull
        public LocalDateTime getStartsAt() { return startsAt; }
        @Nullable
        public LocalDateTime getEndsAt() { return endsAt; }
        @NonNull
        public FarmingCycleStartBasis getStartBasis() { return startBasis; }
        public long getSourceRevision() { return sourceRevision; }
    }

    public record Snapshot(
        @NonNull UUID id,
        @NonNull UUID farmId,
        @NonNull String farmName,
        @NonNull UUID cropId,
        @NonNull String cropName,
        @NonNull FarmingCycleReportStatus status,
        @NonNull LocalDateTime startsAt,
        @Nullable LocalDateTime endsAt,
        @NonNull FarmingCycleStartBasis startBasis,
        @Nullable UUID finalHarvestRecordId,
        int statisticsSchemaVersion,
        long sourceRevision,
        @NonNull CycleReportStatistics statistics
    ) {
        public Snapshot {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(farmName, "farmName");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(startsAt, "startsAt");
            Objects.requireNonNull(startBasis, "startBasis");
            Objects.requireNonNull(statistics, "statistics");
        }

        @NonNull
        public UUID getId() { return id; }
        @NonNull
        public UUID getFarmId() { return farmId; }
        @NonNull
        public String getFarmName() { return farmName; }
        @NonNull
        public UUID getCropId() { return cropId; }
        @NonNull
        public String getCropName() { return cropName; }
        @NonNull
        public FarmingCycleReportStatus getStatus() { return status; }
        @NonNull
        public LocalDateTime getStartsAt() { return startsAt; }
        @Nullable
        public LocalDateTime getEndsAt() { return endsAt; }
        @NonNull
        public FarmingCycleStartBasis getStartBasis() { return startBasis; }
        @Nullable
        public UUID getFinalHarvestRecordId() { return finalHarvestRecordId; }
        public int getStatisticsSchemaVersion() { return statisticsSchemaVersion; }
        public long getSourceRevision() { return sourceRevision; }
        @NonNull
        public CycleReportStatistics getStatistics() { return statistics; }
    }

    public record Detail(
        @NonNull Snapshot selected
    ) {
        public Detail {
            Objects.requireNonNull(selected, "selected");
        }

        @NonNull
        public Snapshot getSelected() { return selected; }
    }

    public record Page(
        @NonNull List<Metadata> items,
        @Nullable String nextCursor
    ) {
        public Page {
            Objects.requireNonNull(items, "items");
        }

        @NonNull
        public List<Metadata> getItems() { return items; }
        @Nullable
        public String getNextCursor() { return nextCursor; }
    }
}
