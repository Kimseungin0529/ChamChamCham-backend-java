package com.chamchamcham.application.coaching.reportfeedback.lifecycle;

import com.chamchamcham.application.coaching.reportfeedback.*;
import com.chamchamcham.application.coaching.reportfeedback.generation.*;
import com.chamchamcham.domain.coaching.reportfeedback.*;
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
import java.util.*;

@Component
public class ReportFeedbackPreparationHandler {
    private static final TypeReference<Map<String, Object>> SNAPSHOT_TYPE = new TypeReference<>() {};
    private final ReportFeedbackRepository feedbackRepository;
    private final ReportFeedbackContextAssembler contextAssembler;
    private final ReportFeedbackContextFingerprint contextFingerprint;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate writeTransaction;
    public ReportFeedbackPreparationHandler(ReportFeedbackRepository feedbackRepository, ReportFeedbackContextAssembler contextAssembler, ReportFeedbackContextFingerprint contextFingerprint, ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher, PlatformTransactionManager transactionManager) { this.feedbackRepository = Objects.requireNonNull(feedbackRepository); this.contextAssembler = Objects.requireNonNull(contextAssembler); this.contextFingerprint = Objects.requireNonNull(contextFingerprint); this.objectMapper = Objects.requireNonNull(objectMapper); this.eventPublisher = Objects.requireNonNull(eventPublisher); writeTransaction = new TransactionTemplate(Objects.requireNonNull(transactionManager)); writeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW); }
    @Async("coachingTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ReportFeedbackPreparationRequested event) {
        Objects.requireNonNull(event, "event");
        var feedback = feedbackRepository.findByIdAndMember_Id(event.getFeedbackId(), event.getMemberId());
        if (feedback == null || !matches(feedback, event)) return;
        var before = feedback.getInputSnapshot();
        Map<String, Object> snapshot = null; ReportFeedbackFailureCode failureCode = null;
        try { var context = contextAssembler.assemble(event.getMemberId(), event.getReportId(), event.getWorkType()); if (!contextFingerprint.calculate(context).equals(event.getSourceFingerprint())) return; snapshot = objectMapper.convertValue(context, SNAPSHOT_TYPE); }
        catch (ReportFeedbackGenerationFailure failure) { failureCode = failure.getCode(); }
        catch (RuntimeException failure) { failureCode = ReportFeedbackFailureCode.CONTEXT_ASSEMBLY_FAILED; }
        var prepared = snapshot; var code = failureCode;
        var request = writeTransaction.execute(status -> {
            var locked = feedbackRepository.findByIdAndMemberIdForUpdate(event.getFeedbackId(), event.getMemberId());
            if (locked == null || !matches(locked, event) || !Objects.equals(locked.getInputSnapshot(), before)) return null;
            if (prepared == null) { if (code == null) throw new IllegalArgumentException("Required value was null."); locked.markFailed(code.name()); return null; }
            locked.attachInputSnapshot(prepared);
            return new ReportFeedbackGenerationRequested(event.getFeedbackId(), event.getMemberId(), event.getReportId(), event.getWorkType(), event.getSourceFingerprint());
        });
        if (request != null) eventPublisher.publishEvent(request);
    }
    private boolean matches(ReportFeedback feedback, ReportFeedbackPreparationRequested event) { return feedback.getStatus() == ReportFeedbackStatus.PENDING && Objects.equals(feedback.getReport().getId(), event.getReportId()) && feedback.getWorkType() == event.getWorkType() && Objects.equals(feedback.getSourceFingerprint(), event.getSourceFingerprint()); }
}
