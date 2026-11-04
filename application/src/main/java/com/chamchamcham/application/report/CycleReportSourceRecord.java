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

public record CycleReportSourceRecord(
    @NonNull UUID id,
    @NonNull LocalDateTime workedAt,
    @NonNull LocalDateTime createdAt,
    @NonNull WorkType workType,
    @NonNull String weatherCondition,
    int weatherTemperature,
    boolean hasPhoto,
    @NonNull String memo,
    @Nullable PlantingReportSource planting,
    @Nullable WateringReportSource watering,
    @Nullable FertilizingReportSource fertilizing,
    @Nullable PestControlReportSource pestControl,
    @Nullable WeedingReportSource weeding,
    @Nullable HarvestReportSource harvest
) {
    public CycleReportSourceRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(workedAt, "workedAt");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(workType, "workType");
        Objects.requireNonNull(weatherCondition, "weatherCondition");
        Objects.requireNonNull(memo, "memo");
    }

    @NonNull
    public UUID getId() { return id; }
    @NonNull
    public LocalDateTime getWorkedAt() { return workedAt; }
    @NonNull
    public LocalDateTime getCreatedAt() { return createdAt; }
    @NonNull
    public WorkType getWorkType() { return workType; }
    @NonNull
    public String getWeatherCondition() { return weatherCondition; }
    public int getWeatherTemperature() { return weatherTemperature; }
    public boolean getHasPhoto() { return hasPhoto; }
    @NonNull
    public String getMemo() { return memo; }
    @Nullable
    public PlantingReportSource getPlanting() { return planting; }
    @Nullable
    public WateringReportSource getWatering() { return watering; }
    @Nullable
    public FertilizingReportSource getFertilizing() { return fertilizing; }
    @Nullable
    public PestControlReportSource getPestControl() { return pestControl; }
    @Nullable
    public WeedingReportSource getWeeding() { return weeding; }
    @Nullable
    public HarvestReportSource getHarvest() { return harvest; }
}
