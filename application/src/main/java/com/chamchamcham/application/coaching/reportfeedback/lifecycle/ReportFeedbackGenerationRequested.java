package com.chamchamcham.application.coaching.reportfeedback.lifecycle;

import com.chamchamcham.application.coaching.reportfeedback.ReportFeedbackFailureCode;
import com.chamchamcham.application.coaching.reportfeedback.ReportFeedbackGenerationFailure;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextAssembler;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextFingerprint;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackRepository;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackStatus;
import com.chamchamcham.domain.farming.WorkType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record ReportFeedbackGenerationRequested(
    @NonNull UUID feedbackId,
    @NonNull UUID memberId,
    @NonNull UUID reportId,
    @NonNull WorkType workType,
    @NonNull String sourceFingerprint
) {
    public ReportFeedbackGenerationRequested {
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
