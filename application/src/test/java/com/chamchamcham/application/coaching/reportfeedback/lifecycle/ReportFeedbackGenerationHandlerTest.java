package com.chamchamcham.application.coaching.reportfeedback.lifecycle;

import com.chamchamcham.application.coaching.common.RagModelInfo;
import com.chamchamcham.application.coaching.reportfeedback.ReportFeedbackFailureCode;
import com.chamchamcham.application.coaching.reportfeedback.ReportFeedbackGenerationFailure;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContent;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContentItem;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContext;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackGenerationResult;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackGenerationService;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackRecord;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackReport;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedback;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackRepository;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackItemSection;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackStatus;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.report.CycleReportStatistics;
import com.chamchamcham.domain.report.FarmingCycleReport;
import com.chamchamcham.domain.report.FarmingCycleReportProjection;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ReportFeedbackGenerationHandlerTest {
    private final UUID memberId = UUID.randomUUID();
    private final UUID reportId = UUID.randomUUID();
    private final UUID feedbackId = UUID.randomUUID();
    private final String sourceFingerprint = "a".repeat(64);
    private final LocalDateTime startsAt = LocalDateTime.of(2026, 3, 1, 9, 0);
    private final LocalDateTime endsAt = LocalDateTime.of(2026, 7, 1, 9, 0);
    @Mock private ReportFeedbackRepository feedbackRepository;
    @Mock private ReportFeedbackGenerationService generationService;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private Member member;
    private FarmingCycleReport report;
    private ReportFeedbackGenerationHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        member = new Member(memberId, "member@example.com", null);
        var farm = new Farm(UUID.randomUUID(), member, "약초농장", "강원도 평창군");
        var crop = new Crop(UUID.randomUUID(), 422, "황기", CropUsePartCategory.ROOT_BARK);
        var finalHarvest = new FarmingRecord(UUID.randomUUID(), member, farm, crop, WorkType.HARVEST, endsAt, "맑음", 24, "최종 수확", com.chamchamcham.domain.farming.EntryMode.MANUAL, false);
        report = also(FarmingCycleReport.create(member, farm, crop, new FarmingCycleReportProjection(FarmingCycleReportStatus.COMPLETED, startsAt, endsAt, FarmingCycleStartBasis.FIRST_RECORD, finalHarvest, 1, CycleReportStatistics.empty())), it -> setId(it, reportId));
        handler = new ReportFeedbackGenerationHandler(feedbackRepository, generationService, objectMapper, new NoOpTransactionManager());
    }

    @Test
    @DisplayName("generation readies only the requested row when the valid result has no items")
    void generation_readies_only_the_requested_row_when_the_valid_result_has_no_items() throws Exception {
        var target = pendingFeedback(WorkType.WATERING, feedbackId, context((WorkType.WATERING)));
        var sibling = pendingFeedback(WorkType.FERTILIZING, UUID.randomUUID(), context(WorkType.FERTILIZING));
        var event = generationEvent(WorkType.WATERING, this.reportId, this.sourceFingerprint);
        stubTarget(event, target);
        var generated = new java.util.concurrent.atomic.AtomicReference<ReportFeedbackContext>();
        when(generationService.generate(anyContext())).thenAnswer(invocation -> {
        generated.set(invocation.getArgument(0));
        return emptyItemResult();
        });
        handler.on(event);
        assertThat(target.getStatus()).isEqualTo(ReportFeedbackStatus.READY);
        assertThat(target.getSummary()).isEqualTo("이번 물 주기 기록의 흐름을 확인했어요.");
        assertThat(target.items()).isEmpty();
        assertThat(sibling.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(generated.get().getWorkType()).isEqualTo(WorkType.WATERING);
        assertThat(generated.get().getRecords()).allMatch(it -> Objects.equals(it.getWorkType(), WorkType.WATERING));
    }

    @Test
    @DisplayName("generation persists comparison rows before the other sections")
    void generation_persists_comparison_rows_before_the_other_sections() throws Exception {
        var target = pendingFeedback(WorkType.WATERING, feedbackId, context((WorkType.WATERING)));
        var event = generationEvent(WorkType.WATERING, this.reportId, this.sourceFingerprint);
        stubTarget(event, target);
        when(generationService.generate(anyContext())).thenReturn(new ReportFeedbackGenerationResult(new ReportFeedbackContent("이번 물 주기 기록의 흐름을 확인했어요.", java.util.Arrays.asList(contentItem("직전보다 기록 1회 증가", "직전 재배보다 물 주기 기록이 한 번 늘었어요.")), java.util.Arrays.asList(contentItem("현재 기록", "물 준 기록을 남겨 흐름을 확인하기 좋았어요.")), List.of(), List.of()), List.of(), List.of(), new RagModelInfo("embedding-test", "chat-test")));
        handler.on(event);
        assertThat(target.items().stream().map(it -> it.getSection()).toList()).containsExactly(ReportFeedbackItemSection.COMPARISON, ReportFeedbackItemSection.STRENGTH);
        assertThat(target.items().stream().map(it -> it.getDisplayOrder()).toList()).containsExactly(0, 1);
        assertThat(target.items().stream().map(it -> it.getText()).toList()).containsExactly("직전 재배보다 물 주기 기록이 한 번 늘었어요.", "물 준 기록을 남겨 흐름을 확인하기 좋았어요.");
    }

    @Test
    @DisplayName("generation ignores an event whose report does not match the target row")
    void generation_ignores_an_event_whose_report_does_not_match_the_target_row() throws Exception {
        var target = pendingFeedback(WorkType.WATERING, feedbackId, context((WorkType.WATERING)));
        var event = generationEvent(WorkType.WATERING, UUID.randomUUID(), this.sourceFingerprint);
        when(feedbackRepository.findByIdAndMember_Id(feedbackId, memberId)).thenReturn(target);
        handler.on(event);
        assertThat(target.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        verify(generationService, never()).generate(anyContext());
        verify(feedbackRepository, never()).findByIdAndMemberIdForUpdate(feedbackId, memberId);
    }

    @Test
    @DisplayName("generation ignores an event whose work type does not match the target row")
    void generation_ignores_an_event_whose_work_type_does_not_match_the_target_row() throws Exception {
        var target = pendingFeedback(WorkType.WATERING, feedbackId, context((WorkType.WATERING)));
        var event = generationEvent(WorkType.FERTILIZING, this.reportId, this.sourceFingerprint);
        when(feedbackRepository.findByIdAndMember_Id(feedbackId, memberId)).thenReturn(target);
        handler.on(event);
        assertThat(target.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        verify(generationService, never()).generate(anyContext());
        verify(feedbackRepository, never()).findByIdAndMemberIdForUpdate(feedbackId, memberId);
    }

    @Test
    @DisplayName("generation ignores an event whose fingerprint does not match the target row")
    void generation_ignores_an_event_whose_fingerprint_does_not_match_the_target_row() throws Exception {
        var target = pendingFeedback(WorkType.WATERING, feedbackId, context((WorkType.WATERING)));
        var event = generationEvent(WorkType.WATERING, this.reportId, "b".repeat(64));
        when(feedbackRepository.findByIdAndMember_Id(feedbackId, memberId)).thenReturn(target);
        handler.on(event);
        assertThat(target.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        verify(generationService, never()).generate(anyContext());
        verify(feedbackRepository, never()).findByIdAndMemberIdForUpdate(feedbackId, memberId);
    }

    @Test
    @DisplayName("generation result is discarded when the snapshot changes before the final write")
    void generation_result_is_discarded_when_the_snapshot_changes_before_the_final_write() throws Exception {
        var target = pendingFeedback(WorkType.WATERING, feedbackId, context((WorkType.WATERING)));
        var event = generationEvent(WorkType.WATERING, this.reportId, this.sourceFingerprint);
        stubTarget(event, target);
        when(generationService.generate(anyContext())).thenAnswer(it -> {
        target.attachInputSnapshot(snapshot(new ReportFeedbackContext(context(WorkType.WATERING).getSchemaVersion(), context(WorkType.WATERING).getWorkType(), context(WorkType.WATERING).getReport(), context(WorkType.WATERING).getRecords(), context(WorkType.WATERING).getPreviousReport(), context(WorkType.WATERING).getComparisons(), java.util.Arrays.asList("snapshot replaced"))));
        return emptyItemResult();
        });
        handler.on(event);
        assertThat(target.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(target.getSummary()).isNull();
    }

    @Test
    @DisplayName("generation result is discarded when the source fingerprint changes before the final write")
    void generation_result_is_discarded_when_the_source_fingerprint_changes_before_the_final_write() throws Exception {
        var target = pendingFeedback(WorkType.WATERING, feedbackId, context((WorkType.WATERING)));
        var event = generationEvent(WorkType.WATERING, this.reportId, this.sourceFingerprint);
        stubTarget(event, target);
        when(generationService.generate(anyContext())).thenAnswer(it -> {
        ReflectionTestUtils.setField(target, "sourceFingerprint", "b".repeat(64));
        return emptyItemResult();
        });
        handler.on(event);
        assertThat(target.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(target.getSummary()).isNull();
    }

    @Test
    @DisplayName("expected generation failure fails only the requested work type row")
    void expected_generation_failure_fails_only_the_requested_work_type_row() throws Exception {
        var target = pendingFeedback(WorkType.WATERING, feedbackId, context((WorkType.WATERING)));
        var sibling = pendingFeedback(WorkType.FERTILIZING, UUID.randomUUID(), context(WorkType.FERTILIZING));
        var event = generationEvent(WorkType.WATERING, this.reportId, this.sourceFingerprint);
        stubTarget(event, target);
        when(generationService.generate(anyContext())).thenThrow(new ReportFeedbackGenerationFailure(ReportFeedbackFailureCode.CHAT_UNAVAILABLE, null));
        handler.on(event);
        assertThat(target.getStatus()).isEqualTo(ReportFeedbackStatus.FAILED);
        assertThat(target.getFailureCode()).isEqualTo(ReportFeedbackFailureCode.CHAT_UNAVAILABLE.name());
        assertThat(sibling.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
    }

    @Test
    @DisplayName("generation failure is discarded when the source fingerprint changes before the final write")
    void generation_failure_is_discarded_when_the_source_fingerprint_changes_before_the_final_write() throws Exception {
        var target = pendingFeedback(WorkType.WATERING, feedbackId, context((WorkType.WATERING)));
        var event = generationEvent(WorkType.WATERING, this.reportId, this.sourceFingerprint);
        stubTarget(event, target);
        when(generationService.generate(anyContext())).thenAnswer(it -> {
        ReflectionTestUtils.setField(target, "sourceFingerprint", "b".repeat(64));
        throw new ReportFeedbackGenerationFailure(ReportFeedbackFailureCode.CHAT_UNAVAILABLE, null);
        });
        handler.on(event);
        assertThat(target.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(target.getFailureCode()).isNull();
    }

    @Test
    @DisplayName("generation failure logs safe diagnostics without raw evidence values")
    void generation_failure_logs_safe_diagnostics_without_raw_evidence_values(CapturedOutput output) throws Exception {
        var target = pendingFeedback(WorkType.WATERING, feedbackId, context((WorkType.WATERING)));
        var event = generationEvent(WorkType.WATERING, this.reportId, this.sourceFingerprint);
        stubTarget(event, target);
        when(generationService.generate(anyContext())).thenThrow(new ReportFeedbackGenerationFailure(ReportFeedbackFailureCode.STRUCTURED_OUTPUT_INVALID, new IllegalStateException("comparison_not_available,strength_count,next_action_text_paragraph,"+
        "summary_text_length,comparison_text_length," +
        "unknown_evidence:untrusted-generated-value")));
        handler.on(event);
        var logs = output.getOut()+ output.getErr();
        assertThat(logs).contains("report feedback generation failed").contains("STRUCTURED_OUTPUT_INVALID").contains("comparison_not_available").contains("strength_count").contains("next_action_text_paragraph").contains("summary_text_length").contains("comparison_text_length").contains("unknown_evidence").doesNotContain("untrusted-generated-value");
    }

    @Test
    @DisplayName("malformed target snapshot fails only that row")
    void malformed_target_snapshot_fails_only_that_row() throws Exception {
        var target = also(new ReportFeedback(feedbackId, member, report, WorkType.WATERING, ReportFeedbackStatus.PENDING, null, sourceFingerprint, List.of(), null, List.of(), null, null, null), it -> it.attachInputSnapshot(map(new AbstractMap.SimpleEntry<>("schemaVersion", "invalid"))));
        var sibling = pendingFeedback(WorkType.FERTILIZING, UUID.randomUUID(), context(WorkType.FERTILIZING));
        var event = generationEvent(WorkType.WATERING, this.reportId, this.sourceFingerprint);
        stubTarget(event, target);
        handler.on(event);
        assertThat(target.getStatus()).isEqualTo(ReportFeedbackStatus.FAILED);
        assertThat(target.getFailureCode()).isEqualTo(ReportFeedbackFailureCode.INVALID_CONTEXT_SNAPSHOT.name());
        assertThat(sibling.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        verify(generationService, never()).generate(anyContext());
    }

    @Test
    @DisplayName("well formed snapshot for a different work type fails as invalid context")
    void well_formed_snapshot_for_a_different_work_type_fails_as_invalid_context() throws Exception {
        var target = pendingFeedback(WorkType.WATERING, feedbackId, context(WorkType.HARVEST));
        var event = generationEvent(WorkType.WATERING, this.reportId, this.sourceFingerprint);
        stubTarget(event, target);
        handler.on(event);
        assertThat(target.getStatus()).isEqualTo(ReportFeedbackStatus.FAILED);
        assertThat(target.getFailureCode()).isEqualTo(ReportFeedbackFailureCode.INVALID_CONTEXT.name());
        verify(generationService, never()).generate(anyContext());
    }
    private ReportFeedback pendingFeedback(WorkType workType, UUID id, ReportFeedbackContext context) {
        return also(new ReportFeedback(id, member, report, workType, ReportFeedbackStatus.PENDING, null, sourceFingerprint, List.of(), null, List.of(), null, null, null), it -> it.attachInputSnapshot(snapshot(context)));
    }
    private ReportFeedbackGenerationRequested generationEvent(WorkType workType, UUID reportId, String sourceFingerprint) {
        return new ReportFeedbackGenerationRequested(feedbackId, memberId, reportId, workType, sourceFingerprint);
    }
    private void stubTarget(ReportFeedbackGenerationRequested event, ReportFeedback target) {
        when(feedbackRepository.findByIdAndMember_Id(event.getFeedbackId(), event.getMemberId())).thenReturn(target);
        when(feedbackRepository.findByIdAndMemberIdForUpdate(event.getFeedbackId(), event.getMemberId())).thenReturn(target);
    }
    private ReportFeedbackContext context(WorkType workType) {
        return new ReportFeedbackContext(ReportFeedbackContext.REPORT_FEEDBACK_CONTEXT_SCHEMA_VERSION, workType, new ReportFeedbackReport(reportId, report.getFarm().getName(), report.getCrop().getName(), startsAt, endsAt, 1, map(new AbstractMap.SimpleEntry<>("recordCount", 1))), java.util.Arrays.asList(new ReportFeedbackRecord(UUID.randomUUID(), startsAt.plusDays(1), workType, "" + workType.name() + " 작업을 기록했어요.", Map.of())), null, List.of(), List.of());
    }
    private ReportFeedbackGenerationResult emptyItemResult() {
        return new ReportFeedbackGenerationResult(new ReportFeedbackContent("이번 물 주기 기록의 흐름을 확인했어요.", List.of(), List.of(), List.of(), List.of()), List.of(), List.of(), new RagModelInfo("embedding-test", "chat-test"));
    }
    private ReportFeedbackContentItem contentItem(String basis, String text) {
        return new ReportFeedbackContentItem(basis, text, List.of());
    }
    private Map<String, Object> snapshot(ReportFeedbackContext context) {
        return objectMapper.convertValue(context, SNAPSHOT_TYPE);
    }
    private ReportFeedbackContext anyContext() {
        return (org.mockito.Mockito.any(ReportFeedbackContext.class) != null ? org.mockito.Mockito.any(ReportFeedbackContext.class) : context(WorkType.WATERING));
    }
    private void setId(Object target, UUID id) { org.springframework.test.util.ReflectionTestUtils.setField(target, "id", id); }
    private static final TypeReference<Map<String, Object>> SNAPSHOT_TYPE = new TypeReference<>() {};
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

}
