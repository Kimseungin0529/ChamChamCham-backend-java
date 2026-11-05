package com.chamchamcham.application.coaching.recordfeedback.lifecycle;

import com.chamchamcham.domain.coaching.recordfeedback.*;
import com.chamchamcham.domain.farming.FarmingRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class RecordFeedbackLifecycleService {
    private final RecordFeedbackRepository feedbackRepository;
    private final ApplicationEventPublisher eventPublisher;
    public RecordFeedbackLifecycleService(RecordFeedbackRepository feedbackRepository, ApplicationEventPublisher eventPublisher) { this.feedbackRepository = Objects.requireNonNull(feedbackRepository); this.eventPublisher = Objects.requireNonNull(eventPublisher); }
    @Transactional
    public RecordFeedback enqueue(FarmingRecord record) {
        Objects.requireNonNull(record, "record");
        UUID recordId = required(record.getId(), "Persisted farming record id is required");
        var parent = feedbackRepository.findRecordByIdForFeedbackUpdate(recordId);
        if (parent == null) throw new IllegalStateException("Farming record not found while allocating feedback revision: " + recordId);
        var previous = feedbackRepository.findAllByRecord_IdOrderBySourceRevisionDesc(recordId);
        long revision = (previous.isEmpty() ? 0L : previous.getFirst().getSourceRevision()) + 1L;
        previous.stream().filter(it -> it.getStatus() == RecordFeedbackStatus.PENDING || it.getStatus() == RecordFeedbackStatus.READY).forEach(RecordFeedback::markStale);
        var saved = feedbackRepository.save(RecordFeedback.pending(parent.getMember(), parent, revision));
        publishPreparation(saved);
        return saved;
    }
    @Transactional
    public void staleFor(UUID recordId) { Objects.requireNonNull(recordId, "recordId"); feedbackRepository.findAllByRecord_IdOrderBySourceRevisionDesc(recordId).stream().filter(it -> it.getStatus() == RecordFeedbackStatus.PENDING || it.getStatus() == RecordFeedbackStatus.READY).forEach(RecordFeedback::markStale); }
    @Transactional
    public RecordFeedback retry(RecordFeedback feedback) { Objects.requireNonNull(feedback, "feedback"); feedback.retry(); var saved = feedbackRepository.save(feedback); publishPreparation(saved); return saved; }
    private void publishPreparation(RecordFeedback feedback) { eventPublisher.publishEvent(new RecordFeedbackPreparationRequested(required(feedback.getId(), "Persisted feedback id is required"), required(feedback.getMember().getId(), "Persisted member id is required"), required(feedback.getRecord().getId(), "Persisted farming record id is required"), feedback.getSourceRevision())); }
    private static UUID required(UUID id, String message) { if (id == null) throw new IllegalArgumentException(message); return id; }
}
