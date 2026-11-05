package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.farming.WorkType;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record ReportFeedbackReport(
    @NonNull UUID id,
    @NonNull String farmName,
    @NonNull String cropName,
    @NonNull LocalDateTime startsAt,
    @NonNull LocalDateTime endsAt,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SourceRevisionDefault.class) long sourceRevision,
    @NonNull Map<String, Object> statistics
) {
    public ReportFeedbackReport {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(farmName, "farmName");
        Objects.requireNonNull(cropName, "cropName");
        Objects.requireNonNull(startsAt, "startsAt");
        Objects.requireNonNull(endsAt, "endsAt");
        Objects.requireNonNull(statistics, "statistics");
    }
    @NonNull public UUID getId() { return id; }
    @NonNull public String getFarmName() { return farmName; }
    @NonNull public String getCropName() { return cropName; }
    @NonNull public LocalDateTime getStartsAt() { return startsAt; }
    @NonNull public LocalDateTime getEndsAt() { return endsAt; }
    public long getSourceRevision() { return sourceRevision; }
    @NonNull public Map<String, Object> getStatistics() { return statistics; }
    public static final class SourceRevisionDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Long> {
        @Override public Long deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Long.class);
        }
        @Override public Long getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 1L; }
        @Override public Long getNullValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 0L; }
    }
}
