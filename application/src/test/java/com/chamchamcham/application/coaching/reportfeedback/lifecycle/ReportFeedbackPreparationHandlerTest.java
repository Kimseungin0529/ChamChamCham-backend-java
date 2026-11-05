package com.chamchamcham.application.coaching.reportfeedback.lifecycle;

import com.chamchamcham.application.coaching.reportfeedback.ReportFeedbackFailureCode;
import com.chamchamcham.application.coaching.reportfeedback.ReportFeedbackGenerationFailure;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContext;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackComparison;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackPreviousReport;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextAssembler;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextFingerprint;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackRecord;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackReport;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedback;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackRepository;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackStatus;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.report.CycleReportStatistics;
import com.chamchamcham.domain.report.Coverage;
import com.chamchamcham.domain.report.FarmingCycleReport;
import com.chamchamcham.domain.report.FarmingCycleReportProjection;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class ReportFeedbackPreparationHandlerTest {
    private final UUID memberId = UUID.randomUUID();
    private final UUID reportId = UUID.randomUUID();
    private final UUID feedbackId = UUID.randomUUID();
    private final UUID recordId = UUID.randomUUID();
    private final UUID previousReportId = UUID.randomUUID();
    private final LocalDateTime startsAt = LocalDateTime.of(2026, 3, 1, 9, 0);
    private final LocalDateTime endsAt = LocalDateTime.of(2026, 7, 1, 9, 0);
    @Mock private ReportFeedbackRepository feedbackRepository;
    @Mock private ReportFeedbackContextAssembler contextAssembler;
    @Mock private ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final ReportFeedbackContextFingerprint contextFingerprint = new ReportFeedbackContextFingerprint(objectMapper);
    private Member member;
    private FarmingCycleReport report;
    private ReportFeedbackPreparationHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        member = new Member(memberId, "member@example.com", null);
        var farm = new Farm(UUID.randomUUID(), member, "약초농장", "강원도 평창군");
        var crop = new Crop(UUID.randomUUID(), 422, "황기", CropUsePartCategory.ROOT_BARK);
        var finalHarvest = new FarmingRecord(UUID.randomUUID(), member, farm, crop, WorkType.HARVEST, endsAt, "맑음", 24, "최종 수확", com.chamchamcham.domain.farming.EntryMode.MANUAL, false);
        report = also(FarmingCycleReport.create(member, farm, crop, new FarmingCycleReportProjection(FarmingCycleReportStatus.COMPLETED, startsAt, endsAt, FarmingCycleStartBasis.FIRST_RECORD, finalHarvest, 1, CycleReportStatistics.empty())), it -> setId(it, reportId));
        handler = new ReportFeedbackPreparationHandler(feedbackRepository, contextAssembler, contextFingerprint, objectMapper, eventPublisher, new NoOpTransactionManager());
    }

    @Test
    @DisplayName("only preparation handler is asynchronous")
    void only_preparation_handler_is_asynchronous() throws Exception {
        var preparationOn = ReportFeedbackPreparationHandler.class.getDeclaredMethod("on", ReportFeedbackPreparationRequested.class);
        var generationOn = ReportFeedbackGenerationHandler.class.getDeclaredMethod("on", ReportFeedbackGenerationRequested.class);
        assertThat(preparationOn.getAnnotation(Async.class).value()).isEqualTo("coachingTaskExecutor");
        assertThat(generationOn.isAnnotationPresent(Async.class)).isFalse();
        assertThat(generationOn.isAnnotationPresent(EventListener.class)).isTrue();
        assertThat(generationOn.isAnnotationPresent(TransactionalEventListener.class)).isFalse();
    }

    @Test
    @DisplayName("preparation publishes generation only after snapshot transaction resources are cleaned up")
    void preparation_publishes_generation_only_after_snapshot_transaction_resources_are_cleaned_up() throws Exception {
        var feedback = pendingFeedback(WorkType.WATERING, feedbackId, (Objects.equals((WorkType.WATERING), WorkType.WATERING) ? sourceFingerprint() : "f".repeat(64)));
        var event = preparationEvent(WorkType.WATERING, this.reportId, sourceFingerprint());
        var transactionManager = new RecordingTransactionManager();
        var transactionAwareHandler = new ReportFeedbackPreparationHandler(feedbackRepository, contextAssembler, contextFingerprint, objectMapper, eventPublisher, transactionManager);
        when(feedbackRepository.findByIdAndMember_Id(feedbackId, memberId)).thenReturn(feedback);
        when(feedbackRepository.findByIdAndMemberIdForUpdate(feedbackId, memberId)).thenReturn(feedback);
        when(contextAssembler.assemble(memberId, reportId, WorkType.WATERING)).thenReturn(context());
        doAnswer(invocation -> {
        assertThat(transactionManager.getResourcesCleanedUp()).isTrue();
        return null;
        }).when(eventPublisher).publishEvent(org.mockito.Mockito.any(ReportFeedbackGenerationRequested.class));
        transactionAwareHandler.on(event);
        assertThat(transactionManager.getResourcesCleanedUp()).isTrue();
        verify(eventPublisher).publishEvent(org.mockito.Mockito.any(ReportFeedbackGenerationRequested.class));
    }

    @Test
    @DisplayName("preparation stores the requested work type snapshot and publishes the same generation target")
    void preparation_stores_the_requested_work_type_snapshot_and_publishes_the_same_generation_target() throws Exception {
        var feedback = pendingFeedback(WorkType.WATERING, feedbackId, (Objects.equals((WorkType.WATERING), WorkType.WATERING) ? sourceFingerprint() : "f".repeat(64)));
        var event = preparationEvent(WorkType.WATERING, this.reportId, sourceFingerprint());
        when(feedbackRepository.findByIdAndMember_Id(feedbackId, memberId)).thenReturn(feedback);
        when(feedbackRepository.findByIdAndMemberIdForUpdate(feedbackId, memberId)).thenReturn(feedback);
        when(contextAssembler.assemble(memberId, reportId, WorkType.WATERING)).thenReturn(context());
        handler.on(event);
        assertThat(feedback.getInputSnapshot()).isNotNull();
        assertThat(feedback.getInputSnapshot().get("schemaVersion")).isEqualTo(3);
        assertThat(nested(feedback.getInputSnapshot(), "report").get("sourceRevision")).isEqualTo(7L);
        assertThat(nested(feedback.getInputSnapshot(), "previousReport").get("sourceRevision")).isEqualTo(3L);
        var comparison = listOfMaps(feedback.getInputSnapshot(), "comparisons").get(0);
        assertThat(comparison.get("metricKey")).isEqualTo("recordCount");
        assertThat(comparison.get("difference")).isEqualTo(new BigDecimal("1"));
        assertThat(nested(comparison, "currentCoverage")).containsEntry("recordedCount", 3).containsEntry("targetCount", 4);
        verify(contextAssembler).assemble(memberId, reportId, WorkType.WATERING);
        var published = ArgumentCaptor.forClass(ReportFeedbackGenerationRequested.class);
        verify(eventPublisher).publishEvent(published.capture());
        assertThat(published.getValue()).isEqualTo(new ReportFeedbackGenerationRequested(feedbackId, memberId, reportId, WorkType.WATERING, sourceFingerprint()));
    }

    @Test
    @DisplayName("preparation preserves an empty comparison list when no previous report exists")
    void preparation_preserves_an_empty_comparison_list_when_no_previous_report_exists() throws Exception {
        var noPrevious = new ReportFeedbackContext(context().getSchemaVersion(), context().getWorkType(), context().getReport(), context().getRecords(), null, List.of(), context().getWarnings());
        var noPreviousFingerprint = contextFingerprint.calculate(noPrevious);
        var feedback = pendingFeedback(WorkType.WATERING, feedbackId, noPreviousFingerprint);
        var event = preparationEvent(WorkType.WATERING, this.reportId, noPreviousFingerprint);
        when(feedbackRepository.findByIdAndMember_Id(feedbackId, memberId)).thenReturn(feedback);
        when(feedbackRepository.findByIdAndMemberIdForUpdate(feedbackId, memberId)).thenReturn(feedback);
        when(contextAssembler.assemble(memberId, reportId, WorkType.WATERING)).thenReturn(noPrevious);
        handler.on(event);
        assertThat(feedback.getInputSnapshot().get("previousReport")).isNull();
        assertThat(feedback.getInputSnapshot().get("comparisons")).isEqualTo(List.of());
    }

    @Test
    @DisplayName("preparation discards an assembled context whose fingerprint no longer matches the request")
    void preparation_discards_an_assembled_context_whose_fingerprint_no_longer_matches_the_request() throws Exception {
        var feedback = pendingFeedback(WorkType.WATERING, feedbackId, (Objects.equals((WorkType.WATERING), WorkType.WATERING) ? sourceFingerprint() : "f".repeat(64)));
        var event = preparationEvent(WorkType.WATERING, this.reportId, sourceFingerprint());
        var changedContext = new ReportFeedbackContext(context().getSchemaVersion(), context().getWorkType(), context().getReport(), context().getRecords(), context().getPreviousReport(), context().getComparisons(), java.util.Arrays.asList("input changed"));
        when(feedbackRepository.findByIdAndMember_Id(feedbackId, memberId)).thenReturn(feedback);
        when(contextAssembler.assemble(memberId, reportId, WorkType.WATERING)).thenReturn(changedContext);
        handler.on(event);
        assertThat(feedback.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(feedback.getInputSnapshot()).isNull();
        verify(feedbackRepository, never()).findByIdAndMemberIdForUpdate(feedbackId, memberId);
        verify(eventPublisher, never()).publishEvent(org.mockito.Mockito.any());
    }

    @Test
    @DisplayName("preparation ignores an event whose report does not match the target row")
    void preparation_ignores_an_event_whose_report_does_not_match_the_target_row() throws Exception {
        var feedback = pendingFeedback(WorkType.WATERING, feedbackId, (Objects.equals((WorkType.WATERING), WorkType.WATERING) ? sourceFingerprint() : "f".repeat(64)));
        var event = preparationEvent(WorkType.WATERING, UUID.randomUUID(), sourceFingerprint());
        when(feedbackRepository.findByIdAndMember_Id(feedbackId, memberId)).thenReturn(feedback);
        handler.on(event);
        assertThat(feedback.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(feedback.getInputSnapshot()).isNull();
        verifyNoInteractions(contextAssembler);
        verify(feedbackRepository, never()).findByIdAndMemberIdForUpdate(feedbackId, memberId);
        verify(eventPublisher, never()).publishEvent(org.mockito.Mockito.any());
    }

    @Test
    @DisplayName("preparation ignores an event whose work type does not match the target row")
    void preparation_ignores_an_event_whose_work_type_does_not_match_the_target_row() throws Exception {
        var feedback = pendingFeedback(WorkType.WATERING, feedbackId, (Objects.equals((WorkType.WATERING), WorkType.WATERING) ? sourceFingerprint() : "f".repeat(64)));
        var event = preparationEvent(WorkType.FERTILIZING, this.reportId, sourceFingerprint());
        when(feedbackRepository.findByIdAndMember_Id(feedbackId, memberId)).thenReturn(feedback);
        handler.on(event);
        assertThat(feedback.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(feedback.getInputSnapshot()).isNull();
        verifyNoInteractions(contextAssembler);
        verify(feedbackRepository, never()).findByIdAndMemberIdForUpdate(feedbackId, memberId);
        verify(eventPublisher, never()).publishEvent(org.mockito.Mockito.any());
    }

    @Test
    @DisplayName("invalid assembled context fails only the requested work type row")
    void invalid_assembled_context_fails_only_the_requested_work_type_row() throws Exception {
        var target = pendingFeedback(WorkType.WATERING, feedbackId, (Objects.equals((WorkType.WATERING), WorkType.WATERING) ? sourceFingerprint() : "f".repeat(64)));
        var sibling = pendingFeedback(WorkType.FERTILIZING, UUID.randomUUID(), (Objects.equals((WorkType.FERTILIZING), WorkType.WATERING) ? sourceFingerprint() : "f".repeat(64)));
        var event = preparationEvent(WorkType.WATERING, this.reportId, sourceFingerprint());
        when(feedbackRepository.findByIdAndMember_Id(feedbackId, memberId)).thenReturn(target);
        when(feedbackRepository.findByIdAndMemberIdForUpdate(feedbackId, memberId)).thenReturn(target);
        when(contextAssembler.assemble(memberId, reportId, WorkType.WATERING)).thenThrow(new ReportFeedbackGenerationFailure(ReportFeedbackFailureCode.INVALID_CONTEXT, null));
        handler.on(event);
        assertThat(target.getStatus()).isEqualTo(ReportFeedbackStatus.FAILED);
        assertThat(target.getFailureCode()).isEqualTo(ReportFeedbackFailureCode.INVALID_CONTEXT.name());
        assertThat(sibling.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        verify(eventPublisher, never()).publishEvent(org.mockito.Mockito.any());
    }

    @Test
    @DisplayName("unexpected assembly failure fails only the requested row with context assembly code")
    void unexpected_assembly_failure_fails_only_the_requested_row_with_context_assembly_code() throws Exception {
        var target = pendingFeedback(WorkType.WATERING, feedbackId, (Objects.equals((WorkType.WATERING), WorkType.WATERING) ? sourceFingerprint() : "f".repeat(64)));
        var sibling = pendingFeedback(WorkType.FERTILIZING, UUID.randomUUID(), (Objects.equals((WorkType.FERTILIZING), WorkType.WATERING) ? sourceFingerprint() : "f".repeat(64)));
        var event = preparationEvent(WorkType.WATERING, this.reportId, sourceFingerprint());
        when(feedbackRepository.findByIdAndMember_Id(feedbackId, memberId)).thenReturn(target);
        when(feedbackRepository.findByIdAndMemberIdForUpdate(feedbackId, memberId)).thenReturn(target);
        when(contextAssembler.assemble(memberId, reportId, WorkType.WATERING)).thenThrow(new IllegalStateException("source unavailable"));
        handler.on(event);
        assertThat(target.getStatus()).isEqualTo(ReportFeedbackStatus.FAILED);
        assertThat(target.getFailureCode()).isEqualTo(ReportFeedbackFailureCode.CONTEXT_ASSEMBLY_FAILED.name());
        assertThat(sibling.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        verify(eventPublisher, never()).publishEvent(org.mockito.Mockito.any());
    }
    private ReportFeedback pendingFeedback(WorkType workType, UUID id, String sourceFingerprint) {
        return new ReportFeedback(id, member, report, workType, ReportFeedbackStatus.PENDING, null, sourceFingerprint, List.of(), null, List.of(), null, null, null);
    }
    private ReportFeedbackPreparationRequested preparationEvent(WorkType workType, UUID reportId, String sourceFingerprint) {
        return new ReportFeedbackPreparationRequested(feedbackId, memberId, reportId, workType, sourceFingerprint);
    }
    private ReportFeedbackContext context() {
        return new ReportFeedbackContext(ReportFeedbackContext.REPORT_FEEDBACK_CONTEXT_SCHEMA_VERSION, WorkType.WATERING, new ReportFeedbackReport(reportId, report.getFarm().getName(), report.getCrop().getName(), startsAt, endsAt, 7, map(new AbstractMap.SimpleEntry<>("recordCount", 1))), java.util.Arrays.asList(new ReportFeedbackRecord(recordId, startsAt.plusDays(1), WorkType.WATERING, "점적관수를 했어요.", Map.of())), new ReportFeedbackPreviousReport(previousReportId, startsAt.minusYears(1), endsAt.minusYears(1), 3, map(new AbstractMap.SimpleEntry<>("recordCount", 3))), java.util.Arrays.asList(new ReportFeedbackComparison("recordCount", "기록 횟수", new BigDecimal("4"), new BigDecimal("3"), new BigDecimal("1"), new BigDecimal("33.33"), "회", new Coverage(3, 4), new Coverage(2, 3))), List.of());
    }
    private String sourceFingerprint() {
        return contextFingerprint.calculate(context());
    }
    private void setId(Object target, UUID id) { org.springframework.test.util.ReflectionTestUtils.setField(target, "id", id); }
    @SafeVarargs
    private static <K> Map<K, Object> map(Map.Entry<K, ?>... entries) {
        Map<K, Object> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private static <T> T also(T value, java.util.function.Consumer<T> action) { action.accept(value); return value; }

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

    @SuppressWarnings("unchecked")
    private Map<String, Object> nested(Map<String, Object> map, String key) { return (Map<String, Object>) map.get(key); }
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOfMaps(Map<String, Object> map, String key) { return (List<Map<String, Object>>) map.get(key); }

}
