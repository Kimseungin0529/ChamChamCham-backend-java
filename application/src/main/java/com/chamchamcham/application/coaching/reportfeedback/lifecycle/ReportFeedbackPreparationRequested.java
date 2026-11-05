package com.chamchamcham.application.coaching.reportfeedback.lifecycle;

import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextAssembler;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextFingerprint;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedback;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackRepository;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackStatus;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.FarmingCycleReport;
import com.chamchamcham.domain.report.FarmingCycleReportRepository;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record ReportFeedbackPreparationRequested(
    @NonNull UUID feedbackId,
    @NonNull UUID memberId,
    @NonNull UUID reportId,
    @NonNull WorkType workType,
    @NonNull String sourceFingerprint
) {
    public ReportFeedbackPreparationRequested {
        Objects.requireNonNull(feedbackId, "feedbackId");
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(reportId, "reportId");
        Objects.requireNonNull(workType, "workType");
        Objects.requireNonNull(sourceFingerprint, "sourceFingerprint");
    }
    @NonNull public UUID getFeedbackId() { return feedbackId; }
    @NonNull public UUID getMemberId() { return memberId; }
    @NonNull public UUID getReportId() { return reportId; }
    @NonNull public WorkType getWorkType() { return workType; }
    @NonNull public String getSourceFingerprint() { return sourceFingerprint; }
}
