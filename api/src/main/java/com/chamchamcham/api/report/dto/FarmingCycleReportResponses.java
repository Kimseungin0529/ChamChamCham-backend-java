package com.chamchamcham.api.report.dto;

import com.chamchamcham.application.report.FarmingCycleReportResult;
import com.chamchamcham.api.report.dto.FarmingReportStatisticsResponses.CycleStatisticsResponse;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class FarmingCycleReportResponses {
    private FarmingCycleReportResponses() {}

    public record MetadataResponse(
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
        public MetadataResponse {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(farmName, "farmName");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(startsAt, "startsAt");
            Objects.requireNonNull(startBasis, "startBasis");
        }
        @NonNull public UUID getId() { return id; }
        @NonNull public UUID getFarmId() { return farmId; }
        @NonNull public String getFarmName() { return farmName; }
        @NonNull public UUID getCropId() { return cropId; }
        @NonNull public String getCropName() { return cropName; }
        @NonNull public FarmingCycleReportStatus getStatus() { return status; }
        @NonNull public LocalDateTime getStartsAt() { return startsAt; }
        @Nullable public LocalDateTime getEndsAt() { return endsAt; }
        @NonNull public FarmingCycleStartBasis getStartBasis() { return startBasis; }
        public long getSourceRevision() { return sourceRevision; }
        public static MetadataResponse from(FarmingCycleReportResult.Metadata source) {
            return new MetadataResponse(
                source.getId(),
                source.getFarmId(),
                source.getFarmName(),
                source.getCropId(),
                source.getCropName(),
                source.getStatus(),
                source.getStartsAt(),
                source.getEndsAt(),
                source.getStartBasis(),
                source.getSourceRevision()
            );
        }
    }

    public record SnapshotResponse(
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
        @NonNull CycleStatisticsResponse statistics
    ) {
        public SnapshotResponse {
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
        @NonNull public UUID getId() { return id; }
        @NonNull public UUID getFarmId() { return farmId; }
        @NonNull public String getFarmName() { return farmName; }
        @NonNull public UUID getCropId() { return cropId; }
        @NonNull public String getCropName() { return cropName; }
        @NonNull public FarmingCycleReportStatus getStatus() { return status; }
        @NonNull public LocalDateTime getStartsAt() { return startsAt; }
        @Nullable public LocalDateTime getEndsAt() { return endsAt; }
        @NonNull public FarmingCycleStartBasis getStartBasis() { return startBasis; }
        @Nullable public UUID getFinalHarvestRecordId() { return finalHarvestRecordId; }
        public int getStatisticsSchemaVersion() { return statisticsSchemaVersion; }
        public long getSourceRevision() { return sourceRevision; }
        @NonNull public CycleStatisticsResponse getStatistics() { return statistics; }
        public static SnapshotResponse from(FarmingCycleReportResult.Snapshot source) {
            return new SnapshotResponse(
                source.getId(),
                source.getFarmId(),
                source.getFarmName(),
                source.getCropId(),
                source.getCropName(),
                source.getStatus(),
                source.getStartsAt(),
                source.getEndsAt(),
                source.getStartBasis(),
                source.getFinalHarvestRecordId(),
                source.getStatisticsSchemaVersion(),
                source.getSourceRevision(),
                CycleStatisticsResponse.from(source.getStatistics())
            );
        }
    }

    public record DetailResponse(
        @NonNull SnapshotResponse selected
    ) {
        public DetailResponse {
            Objects.requireNonNull(selected, "selected");
        }
        @NonNull public SnapshotResponse getSelected() { return selected; }
        public static DetailResponse from(FarmingCycleReportResult.Detail source) {
            return new DetailResponse(
                SnapshotResponse.from(source.getSelected())
            );
        }
    }

    public record PageResponse(
        @NonNull List<MetadataResponse> items,
        @Nullable String nextCursor
    ) {
        public PageResponse {
            Objects.requireNonNull(items, "items");
        }
        @NonNull public List<MetadataResponse> getItems() { return items; }
        @Nullable public String getNextCursor() { return nextCursor; }
        public static PageResponse from(FarmingCycleReportResult.Page source) {
            return new PageResponse(
                source.getItems().stream().map(MetadataResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getNextCursor()
            );
        }
    }
}
