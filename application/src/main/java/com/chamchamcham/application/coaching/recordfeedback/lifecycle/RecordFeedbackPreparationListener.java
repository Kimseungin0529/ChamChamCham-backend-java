package com.chamchamcham.application.coaching.recordfeedback.lifecycle;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RecordFeedbackPreparationListener {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RecordFeedbackPreparationListener.class);
    private final RecordFeedbackPreparationService delegate;
    public RecordFeedbackPreparationListener(RecordFeedbackPreparationService delegate) { this.delegate = Objects.requireNonNull(delegate); }
    @org.springframework.scheduling.annotation.Async("coachingTaskExecutor")
    @org.springframework.transaction.event.TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void on(RecordFeedbackPreparationRequested event) {
        Objects.requireNonNull(event, "event");
        try { delegate.prepare(event); }
        catch (Throwable exception) { logger.error("record feedback context preparation failed", exception); }
    }
}
