package com.chamchamcham.application.coaching.reportfeedback.lifecycle;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedback;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackItemSection;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackRepository;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackStatus;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.FarmingCycleReportRepository;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record ReportFeedbackResultContent(
    @NonNull String summary,
    @NonNull List<ReportFeedbackItemResult> strengths,
    @NonNull List<ReportFeedbackItemResult> improvements,
    @NonNull List<ReportFeedbackItemResult> nextActions,
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ComparisonsDefault.class) List<ReportFeedbackItemResult> comparisons
) {
    public ReportFeedbackResultContent {
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(strengths, "strengths");
        Objects.requireNonNull(improvements, "improvements");
        Objects.requireNonNull(nextActions, "nextActions");
        Objects.requireNonNull(comparisons, "comparisons");
    }
    @NonNull public String getSummary() { return summary; }
    @NonNull public List<ReportFeedbackItemResult> getStrengths() { return strengths; }
    @NonNull public List<ReportFeedbackItemResult> getImprovements() { return improvements; }
    @NonNull public List<ReportFeedbackItemResult> getNextActions() { return nextActions; }
    @NonNull public List<ReportFeedbackItemResult> getComparisons() { return comparisons; }
    public static final class ComparisonsDefault extends com.fasterxml.jackson.databind.JsonDeserializer<List<ReportFeedbackItemResult>> {
        @Override public List<ReportFeedbackItemResult> deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, context.getTypeFactory().constructType(new com.fasterxml.jackson.core.type.TypeReference<List<ReportFeedbackItemResult>>() {}));
        }
        @Override public List<ReportFeedbackItemResult> getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return java.util.Collections.emptyList(); }
    }
}
