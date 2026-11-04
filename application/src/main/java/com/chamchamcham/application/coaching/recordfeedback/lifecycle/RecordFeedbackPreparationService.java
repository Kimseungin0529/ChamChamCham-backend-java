package com.chamchamcham.application.coaching.recordfeedback.lifecycle;

import com.chamchamcham.application.coaching.recordfeedback.RecordFeedbackFailureCode;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextAssembler;
import com.chamchamcham.domain.coaching.recordfeedback.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

@Service
public class RecordFeedbackPreparationService {
    private static final TypeReference<Map<String, Object>> SNAPSHOT_TYPE = new TypeReference<>() {};
    private final RecordFeedbackRepository feedbackRepository;
    private final RecordFeedbackContextAssembler contextAssembler;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate writeTransaction;
    public RecordFeedbackPreparationService(RecordFeedbackRepository feedbackRepository, RecordFeedbackContextAssembler contextAssembler, ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher, PlatformTransactionManager transactionManager) {
        this.feedbackRepository = Objects.requireNonNull(feedbackRepository); this.contextAssembler = Objects.requireNonNull(contextAssembler); this.objectMapper = Objects.requireNonNull(objectMapper); this.eventPublisher = Objects.requireNonNull(eventPublisher); this.writeTransaction = new TransactionTemplate(Objects.requireNonNull(transactionManager)); writeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    public void prepare(RecordFeedbackPreparationRequested event) {
        Objects.requireNonNull(event, "event");
        var current = feedbackRepository.findByIdAndMember_Id(event.getFeedbackId(), event.getMemberId());
        if (current == null || current.getStatus() != RecordFeedbackStatus.PENDING || current.getSourceRevision() != event.getSourceRevision()) return;
        Map<String, Object> snapshot = null; boolean assemblyFailed = false;
        try { snapshot = objectMapper.convertValue(contextAssembler.assemble(event.getMemberId(), event.getRecordId(), event.getSourceRevision()), SNAPSHOT_TYPE); }
        catch (RuntimeException exception) { assemblyFailed = true; }
        var prepared = snapshot; var failed = assemblyFailed;
        var request = writeTransaction.execute(status -> {
            var locked = feedbackRepository.findByIdAndMemberIdForUpdate(event.getFeedbackId(), event.getMemberId());
            if (locked == null || locked.getStatus() != RecordFeedbackStatus.PENDING || !Objects.equals(locked.getRecord().getId(), event.getRecordId()) || locked.getSourceRevision() != event.getSourceRevision()) return null;
            if (failed) { locked.markFailed(RecordFeedbackFailureCode.CONTEXT_ASSEMBLY_FAILED.name()); return null; }
            locked.attachInputSnapshot(prepared);
            return new RecordFeedbackGenerationRequested(event.getFeedbackId(), event.getMemberId(), event.getRecordId(), event.getSourceRevision());
        });
        if (request != null) eventPublisher.publishEvent(request);
    }
}
