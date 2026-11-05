package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.farming.WorkType;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record ReportFeedbackContext(
     int schemaVersion,
    @NonNull WorkType workType,
    @NonNull ReportFeedbackReport report,
    @NonNull List<ReportFeedbackRecord> records,
    @Nullable ReportFeedbackPreviousReport previousReport,
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ComparisonsDefault.class) List<ReportFeedbackComparison> comparisons,
    @NonNull List<String> warnings
) {
    public ReportFeedbackContext {
        Objects.requireNonNull(workType, "workType");
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(records, "records");
        Objects.requireNonNull(comparisons, "comparisons");
        Objects.requireNonNull(warnings, "warnings");
    }
    public int getSchemaVersion() { return schemaVersion; }
    @NonNull public WorkType getWorkType() { return workType; }
    @NonNull public ReportFeedbackReport getReport() { return report; }
    @NonNull public List<ReportFeedbackRecord> getRecords() { return records; }
    @Nullable public ReportFeedbackPreviousReport getPreviousReport() { return previousReport; }
    @NonNull public List<ReportFeedbackComparison> getComparisons() { return comparisons; }
    @NonNull public List<String> getWarnings() { return warnings; }
    public static final int REPORT_FEEDBACK_CONTEXT_SCHEMA_VERSION = 3;
    public static final class ComparisonsDefault extends com.fasterxml.jackson.databind.JsonDeserializer<List<ReportFeedbackComparison>> {
        @Override public List<ReportFeedbackComparison> deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, context.getTypeFactory().constructType(new com.fasterxml.jackson.core.type.TypeReference<List<ReportFeedbackComparison>>() {}));
        }
        @Override public List<ReportFeedbackComparison> getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return java.util.Collections.emptyList(); }
    }
}
