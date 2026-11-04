package com.chamchamcham.application.coaching.recordfeedback.lifecycle;

import com.chamchamcham.application.coaching.recordfeedback.generation.CommonFeedbackDetail;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContext;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextAssembler;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackCropContext;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackFarmContext;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackMemberContext;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackRecordContext;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedback;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackRepository;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackStatus;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

@ExtendWith({MockitoExtension.class})
class RecordFeedbackPreparationServiceTest {
    @Mock private RecordFeedbackRepository feedbackRepository;
    @Mock private RecordFeedbackContextAssembler contextAssembler;
    @Mock private ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final Member member = new Member(UUID.randomUUID(), "member@example.com", null);
    private final Farm farm = new Farm(UUID.randomUUID(), member, "약초밭", "강원도 평창군");
    private final Crop crop = new Crop(UUID.randomUUID(), 1, "황기", CropUsePartCategory.ROOT_BARK);
    private final FarmingRecord record = new FarmingRecord(UUID.randomUUID(), member, farm, crop, WorkType.WATERING, LocalDateTime.of(2026, 7, 11, 9, 0), "맑음", 25, "관수 기록", com.chamchamcham.domain.farming.EntryMode.MANUAL, false);

    @Test
    @DisplayName("prepare stores immutable snapshot and publishes generation request")
    void prepare_stores_immutable_snapshot_and_publishes_generation_request() throws Exception {
        var feedback = feedback();
        var event = event(feedback);
        when(feedbackRepository.findByIdAndMember_Id(event.getFeedbackId(), event.getMemberId())).thenReturn(feedback);
        when(feedbackRepository.findByIdAndMemberIdForUpdate(event.getFeedbackId(), event.getMemberId())).thenReturn(feedback);
        when(contextAssembler.assemble(event.getMemberId(), event.getRecordId(), event.getSourceRevision())).thenReturn(context());
        var service = service();
        service.prepare(event);
        assertThat(feedback.getInputSnapshot()).isNotNull();
        verify(eventPublisher).publishEvent(org.mockito.Mockito.any(RecordFeedbackGenerationRequested.class));
    }

    @Test
    @DisplayName("prepare publishes generation only after snapshot transaction resources are cleaned up")
    void prepare_publishes_generation_only_after_snapshot_transaction_resources_are_cleaned_up() throws Exception {
        var feedback = feedback();
        var event = event(feedback);
        var transactionManager = new RecordingTransactionManager();
        when(feedbackRepository.findByIdAndMember_Id(event.getFeedbackId(), event.getMemberId())).thenReturn(feedback);
        when(feedbackRepository.findByIdAndMemberIdForUpdate(event.getFeedbackId(), event.getMemberId())).thenReturn(feedback);
        when(contextAssembler.assemble(event.getMemberId(), event.getRecordId(), event.getSourceRevision())).thenReturn(context());
        doAnswer(invocation -> {
        assertThat(transactionManager.getResourcesCleanedUp()).isTrue();
        return null;
        }).when(eventPublisher).publishEvent(org.mockito.Mockito.any(RecordFeedbackGenerationRequested.class));
        var service = new RecordFeedbackPreparationService(feedbackRepository, contextAssembler, objectMapper, eventPublisher, transactionManager);
        service.prepare(event);
        assertThat(transactionManager.getResourcesCleanedUp()).isTrue();
        verify(eventPublisher).publishEvent(org.mockito.Mockito.any(RecordFeedbackGenerationRequested.class));
    }

    @Test
    @DisplayName("prepare records context assembly failure")
    void prepare_records_context_assembly_failure() throws Exception {
        var feedback = feedback();
        var event = event(feedback);
        when(feedbackRepository.findByIdAndMember_Id(event.getFeedbackId(), event.getMemberId())).thenReturn(feedback);
        when(feedbackRepository.findByIdAndMemberIdForUpdate(event.getFeedbackId(), event.getMemberId())).thenReturn(feedback);
        when(contextAssembler.assemble(event.getMemberId(), event.getRecordId(), event.getSourceRevision())).thenThrow(new IllegalStateException("weather unavailable"));
        service().prepare(event);
        assertThat(feedback.getStatus()).isEqualTo(RecordFeedbackStatus.FAILED);
        assertThat(feedback.getFailureCode()).isEqualTo("CONTEXT_ASSEMBLY_FAILED");
        verify(eventPublisher, never()).publishEvent(org.mockito.Mockito.any());
    }

    @Test
    @DisplayName("prepare keeps stale feedback stale when context assembly finishes late")
    void prepare_keeps_stale_feedback_stale_when_context_assembly_finishes_late() throws Exception {
        var feedback = feedback();
        var event = event(feedback);
        when(feedbackRepository.findByIdAndMember_Id(event.getFeedbackId(), event.getMemberId())).thenReturn(feedback);
        when(feedbackRepository.findByIdAndMemberIdForUpdate(event.getFeedbackId(), event.getMemberId())).thenReturn(feedback);
        when(contextAssembler.assemble(event.getMemberId(), event.getRecordId(), event.getSourceRevision())).thenAnswer(it -> {
        feedback.markStale();
        return context();
        });
        service().prepare(event);
        assertThat(feedback.getStatus()).isEqualTo(RecordFeedbackStatus.STALE);
        assertThat(feedback.getInputSnapshot()).isNull();
        verify(eventPublisher, never()).publishEvent(org.mockito.Mockito.any());
    }

    @Test
    @DisplayName("prepare propagates fatal errors")
    void prepare_propagates_fatal_errors() throws Exception {
        var feedback = feedback();
        var event = event(feedback);
        when(feedbackRepository.findByIdAndMember_Id(event.getFeedbackId(), event.getMemberId())).thenReturn(feedback);
        when(contextAssembler.assemble(event.getMemberId(), event.getRecordId(), event.getSourceRevision())).thenThrow(new AssertionError("fatal"));
        assertThrows(AssertionError.class, () -> service().prepare(event));
    }
    private RecordFeedbackPreparationService service() {
        return new RecordFeedbackPreparationService(feedbackRepository, contextAssembler, objectMapper, eventPublisher, new NoOpTransactionManager());
    }
    private RecordFeedback feedback() {
        return new RecordFeedback(UUID.randomUUID(), member, record, RecordFeedbackStatus.PENDING, SOURCE_REVISION);
    }
    private RecordFeedbackPreparationRequested event(RecordFeedback feedback) {
        return new RecordFeedbackPreparationRequested(feedback.getId(), member.getId(), record.getId(), feedback.getSourceRevision());
    }
    private RecordFeedbackContext context() {
        return new RecordFeedbackContext(RecordFeedbackContext.RECORD_FEEDBACK_CONTEXT_SCHEMA_VERSION, new RecordFeedbackMemberContext(member.getId(), null, null), new RecordFeedbackFarmContext(farm.getId(), farm.getName(), farm.getRoadAddress(), null, null), new RecordFeedbackCropContext(crop.getId(), crop.getName(), crop.getUsePartCategory()), new RecordFeedbackRecordContext(record.getId(), SOURCE_REVISION, record.getWorkedAt(), record.getWorkType(), CommonFeedbackDetail.INSTANCE, record.getWeatherCondition(), record.getWeatherTemperature(), record.getMemo(), 0), null, List.of());
    }
    private static final long SOURCE_REVISION = 3L;
    private static class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {}
        @Override protected void doCommit(DefaultTransactionStatus status) {}
        @Override protected void doRollback(DefaultTransactionStatus status) {}
    }
    private static final class RecordingTransactionManager extends NoOpTransactionManager {
        private boolean resourcesCleanedUp;
        @Override protected void doCleanupAfterCompletion(Object transaction) { resourcesCleanedUp = true; }
        boolean getResourcesCleanedUp() { return resourcesCleanedUp; }
    }

}
