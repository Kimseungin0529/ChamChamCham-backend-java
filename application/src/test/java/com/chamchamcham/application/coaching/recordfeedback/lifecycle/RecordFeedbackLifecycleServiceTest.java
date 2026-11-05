package com.chamchamcham.application.coaching.recordfeedback.lifecycle;

import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedback;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackRepository;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackStatus;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.inOrder;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class RecordFeedbackLifecycleServiceTest {
    @Mock private RecordFeedbackRepository feedbackRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    private final Member member = new Member(UUID.randomUUID(), "member@example.com", null);
    private final Farm farm = new Farm(UUID.randomUUID(), member, "약초밭", "강원도 평창군");
    private final Crop crop = new Crop(UUID.randomUUID(), 1, "황기", CropUsePartCategory.ROOT_BARK);
    private final FarmingRecord record = new FarmingRecord(UUID.randomUUID(), member, farm, crop, WorkType.WATERING, LocalDateTime.of(2026, 7, 11, 9, 0), "맑음", 25, "관수 기록", com.chamchamcham.domain.farming.EntryMode.MANUAL, false);
    private RecordFeedbackLifecycleService service() { return new RecordFeedbackLifecycleService(feedbackRepository, eventPublisher); }

    @Test
    @DisplayName("enqueue locks the parent record before reading feedback revisions")
    void enqueue_locks_the_parent_record_before_reading_feedback_revisions() throws Exception {
        var recordId = Objects.requireNonNull(record.getId());
        var saved = feedback(UUID.randomUUID(), RecordFeedbackStatus.PENDING, 1);
        when(feedbackRepository.findRecordByIdForFeedbackUpdate(recordId)).thenReturn(record);
        when(feedbackRepository.findAllByRecord_IdOrderBySourceRevisionDesc(recordId)).thenReturn(List.of());
        when(feedbackRepository.save(org.mockito.Mockito.any(RecordFeedback.class))).thenReturn(saved);
        service().enqueue(record);
        var order = inOrder(feedbackRepository);
        order.verify(feedbackRepository).findRecordByIdForFeedbackUpdate(recordId);
        order.verify(feedbackRepository).findAllByRecord_IdOrderBySourceRevisionDesc(recordId);
    }

    @Test
    @DisplayName("enqueue assigns the first feedback revision")
    void enqueue_assigns_the_first_feedback_revision() throws Exception {
        var saved = feedback(UUID.randomUUID(), RecordFeedbackStatus.PENDING, 1);
        when(feedbackRepository.findRecordByIdForFeedbackUpdate(record.getId())).thenReturn(record);
        when(feedbackRepository.findAllByRecord_IdOrderBySourceRevisionDesc(record.getId())).thenReturn(List.of());
        when(feedbackRepository.save(org.mockito.Mockito.any(RecordFeedback.class))).thenReturn(saved);
        var result = service().enqueue(record);
        assertThat(result.getSourceRevision()).isEqualTo(1);
        var eventCaptor = ArgumentCaptor.forClass(RecordFeedbackPreparationRequested.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getSourceRevision()).isEqualTo(1);
    }

    @Test
    @DisplayName("enqueue stales active feedback and increments the feedback revision")
    void enqueue_stales_active_feedback_and_increments_the_feedback_revision() throws Exception {
        var active = feedback(UUID.randomUUID(), RecordFeedbackStatus.READY, 3);
        var saved = feedback(UUID.randomUUID(), RecordFeedbackStatus.PENDING, 4);
        when(feedbackRepository.findRecordByIdForFeedbackUpdate(record.getId())).thenReturn(record);
        when(feedbackRepository.findAllByRecord_IdOrderBySourceRevisionDesc(record.getId())).thenReturn(java.util.Arrays.asList(active));
        when(feedbackRepository.save(org.mockito.Mockito.any(RecordFeedback.class))).thenReturn(saved);
        service().enqueue(record);
        assertThat(active.getStatus()).isEqualTo(RecordFeedbackStatus.STALE);
        var eventCaptor = ArgumentCaptor.forClass(RecordFeedbackPreparationRequested.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getFeedbackId()).isEqualTo(saved.getId());
        assertThat(eventCaptor.getValue().getSourceRevision()).isEqualTo(4);
    }

    @Test
    @DisplayName("retry moves failed feedback back to pending and republishes preparation")
    void retry_moves_failed_feedback_back_to_pending_and_republishes_preparation() throws Exception {
        var failed = feedback(UUID.randomUUID(), RecordFeedbackStatus.PENDING, 1);
        failed.markFailed("RETRIEVAL_FAILED");
        when(feedbackRepository.save(failed)).thenReturn(failed);
        var result = service().retry(failed);
        assertThat(result.getStatus()).isEqualTo(RecordFeedbackStatus.PENDING);
        verify(eventPublisher).publishEvent(org.mockito.Mockito.any(RecordFeedbackPreparationRequested.class));
    }
    private RecordFeedback feedback(UUID id, RecordFeedbackStatus status, long sourceRevision) {
        var feedback = new RecordFeedback(id, member, record, RecordFeedbackStatus.PENDING, sourceRevision);
        if (status == RecordFeedbackStatus.READY) feedback.markReady("관수 기록", "관수 기록이 구체적입니다.", actions(), List.of(), "PASS", List.of(), "chat", "embed");
        return feedback;
    }
    private List<com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackNextActionDraft> actions() {
        return java.util.Arrays.asList(new com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackNextActionDraft(com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionDue.TODAY, com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory.IRRIGATION, "관수 기록", "오늘 토양을 확인하세요."), new com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackNextActionDraft(com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionDue.THIS_WEEK, com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory.CULTIVATION, "배수 기록", "이번 주 배수로를 점검하세요."));
    }
}
