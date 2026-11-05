package com.chamchamcham.application.coaching.reportfeedback.lifecycle;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.coaching.reportfeedback.*;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
public class ReportFeedbackQueryService {
    private final FarmingCycleReportRepository reportRepository;
    private final ReportFeedbackRepository feedbackRepository;
    private final ReportFeedbackLifecycleService lifecycleService;
    public ReportFeedbackQueryService(FarmingCycleReportRepository reportRepository, ReportFeedbackRepository feedbackRepository, ReportFeedbackLifecycleService lifecycleService) { this.reportRepository = Objects.requireNonNull(reportRepository); this.feedbackRepository = Objects.requireNonNull(feedbackRepository); this.lifecycleService = Objects.requireNonNull(lifecycleService); }
    @Transactional(readOnly = true)
    public ReportFeedbackListResult get(UUID memberId, UUID reportId) {
        Objects.requireNonNull(memberId, "memberId"); Objects.requireNonNull(reportId, "reportId");
        var report = reportRepository.findByIdAndMember_Id(reportId, memberId);
        if (report == null || report.getStatus() != FarmingCycleReportStatus.COMPLETED) throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        var feedbacks = new ArrayList<ReportFeedback>();
        for (var it : feedbackRepository.findAllByReport_IdAndMember_Id(reportId, memberId)) if (report.getStatistics().recordCountFor(it.getWorkType()) > 0) feedbacks.add(it);
        feedbacks.sort(Comparator.<ReportFeedback, LocalDate>comparing(it -> { var date = report.getStatistics().lastWorkedOnFor(it.getWorkType()); return date == null ? LocalDate.MIN : date; }).reversed().thenComparingInt(it -> it.getWorkType().ordinal()));
        var results = new ArrayList<ReportFeedbackDetailResult>(); feedbacks.forEach(it -> results.add(toDetailResult(it)));
        return new ReportFeedbackListResult(required(report.getId()), results);
    }
    @Transactional
    public ReportFeedbackDetailResult regenerate(UUID memberId, UUID reportId, WorkType workType) { Objects.requireNonNull(memberId); Objects.requireNonNull(reportId); Objects.requireNonNull(workType); return toDetailResult(lifecycleService.regenerate(memberId, reportId, workType)); }
    private ReportFeedbackDetailResult toDetailResult(ReportFeedback feedback) {
        ReportFeedbackResultContent content = null;
        if (feedback.getStatus() == ReportFeedbackStatus.READY) content = new ReportFeedbackResultContent(required(feedback.getSummary()), itemsFor(feedback, ReportFeedbackItemSection.STRENGTH), itemsFor(feedback, ReportFeedbackItemSection.IMPROVEMENT), itemsFor(feedback, ReportFeedbackItemSection.NEXT_ACTION), itemsFor(feedback, ReportFeedbackItemSection.COMPARISON));
        return new ReportFeedbackDetailResult(required(feedback.getId()), feedback.getWorkType(), feedback.getStatus(), feedback.getInputSnapshot() != null, feedback.getFailureCode(), content, feedback.getCreatedAt(), feedback.getUpdatedAt());
    }
    private List<ReportFeedbackItemResult> itemsFor(ReportFeedback feedback, ReportFeedbackItemSection section) { var result = new ArrayList<ReportFeedbackItemResult>(); for (var it : feedback.items()) if (it.getSection() == section) result.add(new ReportFeedbackItemResult(it.getText())); return result; }
    private static <T> T required(T value) { if (value == null) throw new IllegalArgumentException("Required value was null."); return value; }
}
