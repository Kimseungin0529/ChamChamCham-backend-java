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

public record ReportFeedbackListResult(
    @NonNull UUID reportId,
    @NonNull List<ReportFeedbackDetailResult> feedbacks
) {
    public ReportFeedbackListResult {
        Objects.requireNonNull(reportId, "reportId");
        Objects.requireNonNull(feedbacks, "feedbacks");
    }
    @NonNull public UUID getReportId() { return reportId; }
    @NonNull public List<ReportFeedbackDetailResult> getFeedbacks() { return feedbacks; }
}
