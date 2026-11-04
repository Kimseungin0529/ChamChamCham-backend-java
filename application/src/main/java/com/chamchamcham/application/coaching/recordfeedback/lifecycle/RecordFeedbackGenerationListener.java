package com.chamchamcham.application.coaching.recordfeedback.lifecycle;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RecordFeedbackGenerationListener {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RecordFeedbackGenerationListener.class);
    private final RecordFeedbackGenerationProcessor delegate;
    public RecordFeedbackGenerationListener(RecordFeedbackGenerationProcessor delegate) { this.delegate = Objects.requireNonNull(delegate); }
    @org.springframework.context.event.EventListener
    public void on(RecordFeedbackGenerationRequested event) {
        Objects.requireNonNull(event, "event");
        try { delegate.generate(event); }
        catch (Throwable exception) { logger.error("record feedback generation failed", exception); }
    }
}
