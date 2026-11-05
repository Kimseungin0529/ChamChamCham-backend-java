package com.chamchamcham.application.coaching.reportfeedback.lifecycle;

import com.chamchamcham.application.coaching.reportfeedback.generation.*;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.coaching.reportfeedback.*;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ReportFeedbackLifecycleService {
    private final ReportFeedbackRepository feedbackRepository;
    private final FarmingCycleReportRepository reportRepository;
    private final ReportFeedbackContextAssembler contextAssembler;
    private final ReportFeedbackContextFingerprint contextFingerprint;
    private final ApplicationEventPublisher eventPublisher;
    public ReportFeedbackLifecycleService(ReportFeedbackRepository feedbackRepository, FarmingCycleReportRepository reportRepository, ReportFeedbackContextAssembler contextAssembler, ReportFeedbackContextFingerprint contextFingerprint, ApplicationEventPublisher eventPublisher) { this.feedbackRepository = Objects.requireNonNull(feedbackRepository); this.reportRepository = Objects.requireNonNull(reportRepository); this.contextAssembler = Objects.requireNonNull(contextAssembler); this.contextFingerprint = Objects.requireNonNull(contextFingerprint); this.eventPublisher = Objects.requireNonNull(eventPublisher); }
    @Transactional
    public List<ReportFeedback> reconcile(FarmingCycleReport report, Set<WorkType> workTypes) {
        Objects.requireNonNull(report, "report"); Objects.requireNonNull(workTypes, "workTypes");
        if (report.getStatus() != FarmingCycleReportStatus.COMPLETED) throw new IllegalArgumentException("only completed reports can reconcile feedback");
        var reportId = required(report.getId(), "Persisted farming cycle report id is required");
        var memberId = required(report.getMember().getId(), "Persisted member id is required");
        var existing = feedbackRepository.findAllByReportAndMemberForUpdate(reportId, memberId);
        var ordered = new ArrayList<WorkType>(); for (var type : WorkType.values()) if (workTypes.contains(type)) ordered.add(type);
        if (existing.isEmpty()) {
            var pending = new ArrayList<ReportFeedback>();
            for (var type : ordered) pending.add(ReportFeedback.pending(report.getMember(), report, type, contextFingerprint.calculate(contextAssembler.assemble(memberId, reportId, type))));
            var saved = feedbackRepository.saveAll(pending); saved.forEach(this::publishPreparation); return saved;
        }
        var existingByType = new LinkedHashMap<WorkType, ReportFeedback>(); existing.forEach(it -> existingByType.put(it.getWorkType(), it));
        var current = new ArrayList<ReportFeedback>();
        for (var type : ordered) {
            var feedback = existingByType.get(type);
            if (feedback == null) current.add(ReportFeedback.stalePlaceholder(report.getMember(), report, type));
            else { reconcileFingerprint(feedback, contextFingerprint.calculate(contextAssembler.assemble(memberId, reportId, type))); current.add(feedback); }
        }
        for (var feedback : existing) if (!workTypes.contains(feedback.getWorkType())) { feedback.markStale(); current.add(feedback); }
        var saved = feedbackRepository.saveAll(current);
        if (saved.isEmpty()) return Collections.emptyList();
        if (saved.size() == 1) return Collections.singletonList(saved.getFirst());
        var sorted = saved.toArray(ReportFeedback[]::new); Arrays.sort(sorted, Comparator.comparingInt(it -> it.getWorkType().ordinal())); return Arrays.asList(sorted);
    }
    @Transactional
    public ReportFeedback regenerate(UUID memberId, UUID reportId, WorkType workType) {
        Objects.requireNonNull(memberId); Objects.requireNonNull(reportId); Objects.requireNonNull(workType);
        var report = reportRepository.findByIdAndMemberIdForUpdate(reportId, memberId);
        if (report == null || report.getStatus() != FarmingCycleReportStatus.COMPLETED) throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        if (report.getStatistics().recordCountFor(workType) == 0) throw new BusinessException(ErrorCode.REPORT_FEEDBACK_NOT_FOUND);
        var feedback = feedbackRepository.findByReportAndWorkTypeForUpdate(reportId, memberId, workType);
        if (feedback == null) throw new BusinessException(ErrorCode.REPORT_FEEDBACK_NOT_FOUND);
        if (feedback.getStatus() != ReportFeedbackStatus.FAILED && feedback.getStatus() != ReportFeedbackStatus.STALE) throw new BusinessException(ErrorCode.REPORT_FEEDBACK_REGENERATION_NOT_ALLOWED);
        feedback.retry(contextFingerprint.calculate(contextAssembler.assemble(memberId, report, workType))); publishPreparation(feedback); return feedback;
    }
    private void reconcileFingerprint(ReportFeedback feedback, String fingerprint) {
        var stored = feedback.getSourceFingerprint();
        if (stored != null) { if (!stored.equals(fingerprint)) feedback.markStale(); return; }
        String snapshotFingerprint = null;
        if (feedback.getInputSnapshot() != null) try { snapshotFingerprint = contextFingerprint.calculate(feedback.getInputSnapshot()); } catch (Throwable ignored) { }
        if (snapshotFingerprint != null && snapshotFingerprint.equals(fingerprint)) feedback.backfillSourceFingerprint(snapshotFingerprint); else feedback.markStale();
    }
    private void publishPreparation(ReportFeedback feedback) { eventPublisher.publishEvent(new ReportFeedbackPreparationRequested(required(feedback.getId(), "Persisted report feedback id is required"), required(feedback.getMember().getId(), "Persisted member id is required"), required(feedback.getReport().getId(), "Persisted report id is required"), feedback.getWorkType(), required(feedback.getSourceFingerprint(), "Pending report feedback source fingerprint is required"))); }
    private static <T> T required(T value, String message) { if (value == null) throw new IllegalArgumentException(message); return value; }
}
