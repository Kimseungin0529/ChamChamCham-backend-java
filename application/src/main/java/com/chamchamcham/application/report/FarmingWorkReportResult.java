package com.chamchamcham.application.report;

import com.chamchamcham.application.coaching.reportfeedback.lifecycle.ReportFeedbackResultContent;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackStatus;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.CommonOnlyStatistics;
import com.chamchamcham.domain.report.FertilizingStatistics;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.HarvestStatistics;
import com.chamchamcham.domain.report.PestControlStatistics;
import com.chamchamcham.domain.report.PlantingStatistics;
import com.chamchamcham.domain.report.WateringStatistics;
import com.chamchamcham.domain.report.WeedingStatistics;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class FarmingWorkReportResult {
    private FarmingWorkReportResult() {}

    public record Item(
        @NonNull UUID reportId,
        @NonNull FarmingCycleReportStatus status,
        @NonNull UUID farmId,
        @NonNull String farmName,
        @NonNull UUID cropId,
        @NonNull String cropName,
        @NonNull LocalDateTime startsAt,
        @Nullable LocalDateTime endsAt,
        @Nullable UUID finalHarvestRecordId,
        @NonNull WorkType workType,
        @NonNull String workTypeLabel,
        int recordCount,
        @Nullable LocalDate lastWorkedOn,
        @Nullable String thumbnailUrl
    ) {
        public Item {
            Objects.requireNonNull(reportId, "reportId");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(farmName, "farmName");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
            Objects.requireNonNull(startsAt, "startsAt");
            Objects.requireNonNull(workType, "workType");
            Objects.requireNonNull(workTypeLabel, "workTypeLabel");
        }

        @NonNull
        public UUID getReportId() { return reportId; }
        @NonNull
        public FarmingCycleReportStatus getStatus() { return status; }
        @NonNull
        public UUID getFarmId() { return farmId; }
        @NonNull
        public String getFarmName() { return farmName; }
        @NonNull
        public UUID getCropId() { return cropId; }
        @NonNull
        public String getCropName() { return cropName; }
        @NonNull
        public LocalDateTime getStartsAt() { return startsAt; }
        @Nullable
        public LocalDateTime getEndsAt() { return endsAt; }
        @Nullable
        public UUID getFinalHarvestRecordId() { return finalHarvestRecordId; }
        @NonNull
        public WorkType getWorkType() { return workType; }
        @NonNull
        public String getWorkTypeLabel() { return workTypeLabel; }
        public int getRecordCount() { return recordCount; }
        @Nullable
        public LocalDate getLastWorkedOn() { return lastWorkedOn; }
        @Nullable
        public String getThumbnailUrl() { return thumbnailUrl; }
    }

    public record Page(
        @NonNull List<Item> items,
        @Nullable String nextCursor
    ) {
        public Page {
            Objects.requireNonNull(items, "items");
        }

        @NonNull
        public List<Item> getItems() { return items; }
        @Nullable
        public String getNextCursor() { return nextCursor; }
    }

    public record Detail(
        @NonNull UUID reportId,
        @NonNull FarmingCycleReportStatus status,
        @NonNull WorkType workType,
        @NonNull String workTypeLabel,
        @NonNull UUID farmId,
        @NonNull String farmName,
        @NonNull UUID cropId,
        @NonNull String cropName,
        @NonNull LocalDateTime startsAt,
        @Nullable LocalDateTime endsAt,
        @NonNull WorkStatistics statistics,
        @Nullable FeedbackStatus feedback
    ) {
        public Detail {
            Objects.requireNonNull(reportId, "reportId");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(workType, "workType");
            Objects.requireNonNull(workTypeLabel, "workTypeLabel");
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(farmName, "farmName");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
            Objects.requireNonNull(startsAt, "startsAt");
            Objects.requireNonNull(statistics, "statistics");
        }

        @NonNull
        public UUID getReportId() { return reportId; }
        @NonNull
        public FarmingCycleReportStatus getStatus() { return status; }
        @NonNull
        public WorkType getWorkType() { return workType; }
        @NonNull
        public String getWorkTypeLabel() { return workTypeLabel; }
        @NonNull
        public UUID getFarmId() { return farmId; }
        @NonNull
        public String getFarmName() { return farmName; }
        @NonNull
        public UUID getCropId() { return cropId; }
        @NonNull
        public String getCropName() { return cropName; }
        @NonNull
        public LocalDateTime getStartsAt() { return startsAt; }
        @Nullable
        public LocalDateTime getEndsAt() { return endsAt; }
        @NonNull
        public WorkStatistics getStatistics() { return statistics; }
        @Nullable
        public FeedbackStatus getFeedback() { return feedback; }
    }

    public record WorkStatistics(
        @NonNull CommonOnlyStatistics common,
        @Nullable PlantingStatistics planting,
        @Nullable WateringStatistics watering,
        @Nullable FertilizingStatistics fertilizing,
        @Nullable PestControlStatistics pestControl,
        @Nullable WeedingStatistics weeding,
        @Nullable HarvestStatistics harvest
    ) {
        public WorkStatistics {
            Objects.requireNonNull(common, "common");
        }

        @NonNull
        public CommonOnlyStatistics getCommon() { return common; }
        @Nullable
        public PlantingStatistics getPlanting() { return planting; }
        @Nullable
        public WateringStatistics getWatering() { return watering; }
        @Nullable
        public FertilizingStatistics getFertilizing() { return fertilizing; }
        @Nullable
        public PestControlStatistics getPestControl() { return pestControl; }
        @Nullable
        public WeedingStatistics getWeeding() { return weeding; }
        @Nullable
        public HarvestStatistics getHarvest() { return harvest; }
    }

    public record FeedbackStatus(
        @NonNull ReportFeedbackStatus status,
        @Nullable ReportFeedbackResultContent content
    ) {
        public FeedbackStatus {
            Objects.requireNonNull(status, "status");
        }

        @NonNull
        public ReportFeedbackStatus getStatus() { return status; }
        @Nullable
        public ReportFeedbackResultContent getContent() { return content; }
    }
}
