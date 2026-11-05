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

public record ReportFeedbackDetailResult(
    @NonNull UUID feedbackId,
    @NonNull WorkType workType,
    @NonNull ReportFeedbackStatus status,
     boolean inputPrepared,
    @Nullable String failureCode,
    @Nullable ReportFeedbackResultContent content,
    @NonNull LocalDateTime createdAt,
    @NonNull LocalDateTime updatedAt
) {
    public ReportFeedbackDetailResult {
        Objects.requireNonNull(feedbackId, "feedbackId");
        Objects.requireNonNull(workType, "workType");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
    @NonNull public UUID getFeedbackId() { return feedbackId; }
    @NonNull public WorkType getWorkType() { return workType; }
    @NonNull public ReportFeedbackStatus getStatus() { return status; }
    public boolean getInputPrepared() { return inputPrepared; }
    @Nullable public String getFailureCode() { return failureCode; }
    @Nullable public ReportFeedbackResultContent getContent() { return content; }
    @NonNull public LocalDateTime getCreatedAt() { return createdAt; }
    @NonNull public LocalDateTime getUpdatedAt() { return updatedAt; }
}
