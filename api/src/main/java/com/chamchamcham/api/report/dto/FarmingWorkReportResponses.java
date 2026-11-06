package com.chamchamcham.api.report.dto;

import com.chamchamcham.api.report.dto.FarmingReportStatisticsResponses.CommonStatisticsResponse;
import com.chamchamcham.api.report.dto.FarmingReportStatisticsResponses.FertilizingStatisticsResponse;
import com.chamchamcham.api.report.dto.FarmingReportStatisticsResponses.HarvestStatisticsResponse;
import com.chamchamcham.api.report.dto.FarmingReportStatisticsResponses.PestControlStatisticsResponse;
import com.chamchamcham.api.report.dto.FarmingReportStatisticsResponses.PlantingStatisticsResponse;
import com.chamchamcham.api.report.dto.FarmingReportStatisticsResponses.WateringStatisticsResponse;
import com.chamchamcham.api.report.dto.FarmingReportStatisticsResponses.WeedingStatisticsResponse;
import com.chamchamcham.application.coaching.reportfeedback.lifecycle.ReportFeedbackItemResult;
import com.chamchamcham.application.coaching.reportfeedback.lifecycle.ReportFeedbackResultContent;
import com.chamchamcham.application.report.FarmingWorkReportResult;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackStatus;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class FarmingWorkReportResponses {
    private FarmingWorkReportResponses() {}

    public record ItemResponse(
        @NonNull UUID reportId,
        @NonNull FarmingCycleReportStatus status,
        @NonNull UUID farmId,
        @NonNull String farmName,
        @NonNull UUID cropId,
        @NonNull String cropName,
        @NonNull LocalDateTime startsAt,
        @Nullable LocalDateTime endsAt,
        @NonNull WorkType workType,
        @NonNull String workTypeLabel,
        int recordCount,
        @Nullable LocalDate lastWorkedOn,
        @Nullable String thumbnailUrl
    ) {
        public ItemResponse {
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
        @NonNull public UUID getReportId() { return reportId; }
        @NonNull public FarmingCycleReportStatus getStatus() { return status; }
        @NonNull public UUID getFarmId() { return farmId; }
        @NonNull public String getFarmName() { return farmName; }
        @NonNull public UUID getCropId() { return cropId; }
        @NonNull public String getCropName() { return cropName; }
        @NonNull public LocalDateTime getStartsAt() { return startsAt; }
        @Nullable public LocalDateTime getEndsAt() { return endsAt; }
        @NonNull public WorkType getWorkType() { return workType; }
        @NonNull public String getWorkTypeLabel() { return workTypeLabel; }
        public int getRecordCount() { return recordCount; }
        @Nullable public LocalDate getLastWorkedOn() { return lastWorkedOn; }
        @Nullable public String getThumbnailUrl() { return thumbnailUrl; }
        public static ItemResponse from(FarmingWorkReportResult.Item source) {
            return new ItemResponse(
                source.getReportId(),
                source.getStatus(),
                source.getFarmId(),
                source.getFarmName(),
                source.getCropId(),
                source.getCropName(),
                source.getStartsAt(),
                source.getEndsAt(),
                source.getWorkType(),
                source.getWorkTypeLabel(),
                source.getRecordCount(),
                source.getLastWorkedOn(),
                source.getThumbnailUrl()
            );
        }
    }

    public record PageResponse(
        @NonNull List<ItemResponse> items,
        @Nullable String nextCursor
    ) {
        public PageResponse {
            Objects.requireNonNull(items, "items");
        }
        @NonNull public List<ItemResponse> getItems() { return items; }
        @Nullable public String getNextCursor() { return nextCursor; }
        public static PageResponse from(FarmingWorkReportResult.Page source) {
            return new PageResponse(
                source.getItems().stream().map(ItemResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getNextCursor()
            );
        }
    }

    public record DetailResponse(
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
        @NonNull StatisticsResponse statistics,
        @Nullable FeedbackStatusResponse feedback
    ) {
        public DetailResponse {
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
        @NonNull public UUID getReportId() { return reportId; }
        @NonNull public FarmingCycleReportStatus getStatus() { return status; }
        @NonNull public WorkType getWorkType() { return workType; }
        @NonNull public String getWorkTypeLabel() { return workTypeLabel; }
        @NonNull public UUID getFarmId() { return farmId; }
        @NonNull public String getFarmName() { return farmName; }
        @NonNull public UUID getCropId() { return cropId; }
        @NonNull public String getCropName() { return cropName; }
        @NonNull public LocalDateTime getStartsAt() { return startsAt; }
        @Nullable public LocalDateTime getEndsAt() { return endsAt; }
        @NonNull public StatisticsResponse getStatistics() { return statistics; }
        @Nullable public FeedbackStatusResponse getFeedback() { return feedback; }
        public static DetailResponse from(FarmingWorkReportResult.Detail source) {
            return new DetailResponse(
                source.getReportId(),
                source.getStatus(),
                source.getWorkType(),
                source.getWorkTypeLabel(),
                source.getFarmId(),
                source.getFarmName(),
                source.getCropId(),
                source.getCropName(),
                source.getStartsAt(),
                source.getEndsAt(),
                StatisticsResponse.from(source.getStatistics()),
                (source.getFeedback() == null ? null : FeedbackStatusResponse.from(source.getFeedback()))
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StatisticsResponse(
        @NonNull CommonStatisticsResponse common,
        @Nullable PlantingStatisticsResponse planting,
        @Nullable WateringStatisticsResponse watering,
        @Nullable FertilizingStatisticsResponse fertilizing,
        @Nullable PestControlStatisticsResponse pestControl,
        @Nullable WeedingStatisticsResponse weeding,
        @Nullable HarvestStatisticsResponse harvest
    ) {
        public StatisticsResponse {
            Objects.requireNonNull(common, "common");
        }
        @NonNull public CommonStatisticsResponse getCommon() { return common; }
        @Nullable public PlantingStatisticsResponse getPlanting() { return planting; }
        @Nullable public WateringStatisticsResponse getWatering() { return watering; }
        @Nullable public FertilizingStatisticsResponse getFertilizing() { return fertilizing; }
        @Nullable public PestControlStatisticsResponse getPestControl() { return pestControl; }
        @Nullable public WeedingStatisticsResponse getWeeding() { return weeding; }
        @Nullable public HarvestStatisticsResponse getHarvest() { return harvest; }
        public static StatisticsResponse from(FarmingWorkReportResult.WorkStatistics source) {
            return new StatisticsResponse(
                CommonStatisticsResponse.from(source.getCommon()),
                (source.getPlanting() == null ? null : PlantingStatisticsResponse.from(source.getPlanting())),
                (source.getWatering() == null ? null : WateringStatisticsResponse.from(source.getWatering())),
                (source.getFertilizing() == null ? null : FertilizingStatisticsResponse.from(source.getFertilizing())),
                (source.getPestControl() == null ? null : PestControlStatisticsResponse.from(source.getPestControl())),
                (source.getWeeding() == null ? null : WeedingStatisticsResponse.from(source.getWeeding())),
                (source.getHarvest() == null ? null : HarvestStatisticsResponse.from(source.getHarvest()))
            );
        }
    }

    public record FeedbackStatusResponse(
        @NonNull ReportFeedbackStatus status,
        @Nullable FeedbackContentResponse content
    ) {
        public FeedbackStatusResponse {
            Objects.requireNonNull(status, "status");
        }
        @NonNull public ReportFeedbackStatus getStatus() { return status; }
        @Nullable public FeedbackContentResponse getContent() { return content; }
        public static FeedbackStatusResponse from(FarmingWorkReportResult.FeedbackStatus source) {
            return new FeedbackStatusResponse(
                source.getStatus(),
                (source.getContent() == null ? null : FeedbackContentResponse.from(source.getContent()))
            );
        }
    }

    public record FeedbackContentResponse(
        @NonNull String summary,
        @NonNull List<FeedbackItemResponse> comparisons,
        @NonNull List<FeedbackItemResponse> strengths,
        @NonNull List<FeedbackItemResponse> improvements,
        @NonNull List<FeedbackItemResponse> nextActions
    ) {
        public FeedbackContentResponse {
            Objects.requireNonNull(summary, "summary");
            Objects.requireNonNull(comparisons, "comparisons");
            Objects.requireNonNull(strengths, "strengths");
            Objects.requireNonNull(improvements, "improvements");
            Objects.requireNonNull(nextActions, "nextActions");
        }
        @NonNull public String getSummary() { return summary; }
        @NonNull public List<FeedbackItemResponse> getComparisons() { return comparisons; }
        @NonNull public List<FeedbackItemResponse> getStrengths() { return strengths; }
        @NonNull public List<FeedbackItemResponse> getImprovements() { return improvements; }
        @NonNull public List<FeedbackItemResponse> getNextActions() { return nextActions; }
        public static FeedbackContentResponse from(ReportFeedbackResultContent source) {
            return new FeedbackContentResponse(
                source.getSummary(),
                source.getComparisons().stream().map(FeedbackItemResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getStrengths().stream().map(FeedbackItemResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getImprovements().stream().map(FeedbackItemResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getNextActions().stream().map(FeedbackItemResponse::from).collect(Collectors.toCollection(ArrayList::new))
            );
        }
    }

    public record FeedbackItemResponse(
        @NonNull String text
    ) {
        public FeedbackItemResponse {
            Objects.requireNonNull(text, "text");
        }
        @NonNull public String getText() { return text; }
        public static FeedbackItemResponse from(ReportFeedbackItemResult source) {
            return new FeedbackItemResponse(
                source.getText()
            );
        }
    }
}
