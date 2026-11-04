package com.chamchamcham.application.coaching.common;

import com.chamchamcham.application.coaching.recordfeedback.RecordFeedbackFailureCode;
import com.chamchamcham.application.coaching.reportfeedback.ReportFeedbackFailureCode;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackRepository;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Component
public class PendingFeedbackExpiryScheduler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PendingFeedbackExpiryScheduler.class);
    private final RecordFeedbackRepository recordFeedbackRepository;
    private final ReportFeedbackRepository reportFeedbackRepository;
    private final RagProperties ragProperties;
    public PendingFeedbackExpiryScheduler(RecordFeedbackRepository recordFeedbackRepository, ReportFeedbackRepository reportFeedbackRepository, RagProperties ragProperties) { this.recordFeedbackRepository = Objects.requireNonNull(recordFeedbackRepository); this.reportFeedbackRepository = Objects.requireNonNull(reportFeedbackRepository); this.ragProperties = Objects.requireNonNull(ragProperties); }
    @Scheduled(fixedDelayString = "${rag.execution.scan-interval-millis:60000}")
    @Transactional
    public void expirePending() {
        var failedAt = LocalDateTime.now();
        var cutoff = failedAt.minus(Duration.ofMillis(ragProperties.getExecution().getPendingTimeoutMillis()));
        int recordCount = recordFeedbackRepository.failPendingUpdatedBefore(cutoff, failedAt, RecordFeedbackFailureCode.PROCESSING_TIMEOUT.name());
        int reportCount = reportFeedbackRepository.failPendingUpdatedBefore(cutoff, failedAt, ReportFeedbackFailureCode.PROCESSING_TIMEOUT.name());
        if (recordCount + reportCount > 0) logger.warn("expired pending coaching feedback recordCount={} reportCount={}", recordCount, reportCount);
    }
}
