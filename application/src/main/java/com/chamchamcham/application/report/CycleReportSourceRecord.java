package com.chamchamcham.application.report;

import com.chamchamcham.domain.farming.WorkType;
import java.time.LocalDateTime;
import java.util.UUID;
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

    @com.fasterxml.jackson.annotation.JsonCreator
    public static CycleReportSourceRecord fromJson(
            @com.fasterxml.jackson.annotation.JsonProperty("id") UUID id,
            @com.fasterxml.jackson.annotation.JsonProperty("workedAt") LocalDateTime workedAt,
            @com.fasterxml.jackson.annotation.JsonProperty("createdAt")
            @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = CreatedAtInputDeserializer.class) CreatedAtInput createdAt,
            @com.fasterxml.jackson.annotation.JsonProperty("workType") WorkType workType,
            @com.fasterxml.jackson.annotation.JsonProperty("weatherCondition") String weatherCondition,
            @com.fasterxml.jackson.annotation.JsonProperty("weatherTemperature") int weatherTemperature,
            @com.fasterxml.jackson.annotation.JsonProperty("hasPhoto") boolean hasPhoto,
            @com.fasterxml.jackson.annotation.JsonProperty("memo")
            @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = MemoWithDefault.class) String memo,
            @com.fasterxml.jackson.annotation.JsonProperty("planting") PlantingReportSource planting,
            @com.fasterxml.jackson.annotation.JsonProperty("watering") WateringReportSource watering,
            @com.fasterxml.jackson.annotation.JsonProperty("fertilizing") FertilizingReportSource fertilizing,
            @com.fasterxml.jackson.annotation.JsonProperty("pestControl") PestControlReportSource pestControl,
            @com.fasterxml.jackson.annotation.JsonProperty("weeding") WeedingReportSource weeding,
            @com.fasterxml.jackson.annotation.JsonProperty("harvest") HarvestReportSource harvest) {
        return new CycleReportSourceRecord(id, workedAt, createdAt == null ? workedAt : createdAt.value(), workType,
            weatherCondition, weatherTemperature, hasPhoto, memo, planting, watering, fertilizing, pestControl, weeding, harvest);
    }

    public record CreatedAtInput(LocalDateTime value) {}

    // createdAt의 누락은 workedAt을 사용하지만 명시적 null은 거부한다.
    public static class CreatedAtInputDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<CreatedAtInput> {
        @Override
        public CreatedAtInput deserialize(com.fasterxml.jackson.core.JsonParser parser,
            com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return new CreatedAtInput(context.readValue(parser, LocalDateTime.class));
        }
        @Override public CreatedAtInput getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return null; }
        @Override public CreatedAtInput getNullValue(com.fasterxml.jackson.databind.DeserializationContext context) { return new CreatedAtInput(null); }
    }

    public static class MemoWithDefault extends com.fasterxml.jackson.databind.JsonDeserializer<String> {
        @Override
        public String deserialize(com.fasterxml.jackson.core.JsonParser parser,
            com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, String.class);
        }
        @Override public String getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return ""; }
    }
}
