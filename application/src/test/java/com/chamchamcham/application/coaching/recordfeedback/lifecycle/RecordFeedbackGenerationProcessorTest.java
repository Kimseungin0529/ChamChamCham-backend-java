package com.chamchamcham.application.coaching.recordfeedback.lifecycle;

import com.chamchamcham.application.coaching.common.RagModelInfo;
import com.chamchamcham.application.coaching.recordfeedback.RecordFeedbackFailureCode;
import com.chamchamcham.application.coaching.recordfeedback.RecordFeedbackGenerationFailure;
import com.chamchamcham.application.coaching.recordfeedback.generation.CommonFeedbackDetail;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackAction;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContent;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContext;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackCropContext;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackFarmContext;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackGenerationResult;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackGenerationService;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackGoodPoint;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackMemberContext;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackRecordContext;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedback;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionDue;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackRepository;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackStatus;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class RecordFeedbackGenerationProcessorTest {
    @Mock private RecordFeedbackRepository feedbackRepository;
    @Mock private RecordFeedbackGenerationService generationService;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final Member member = new Member(UUID.randomUUID(), "member@example.com", null);
    private final Farm farm = new Farm(UUID.randomUUID(), member, "약초밭", "강원도 평창군");
    private final Crop crop = new Crop(UUID.randomUUID(), 1, "황기", CropUsePartCategory.ROOT_BARK);
    private final FarmingRecord record = new FarmingRecord(UUID.randomUUID(), member, farm, crop, WorkType.PRUNING, LocalDateTime.of(2026, 7, 11, 9, 0), "맑음", 25, "웃자란 가지를 정리했습니다.", com.chamchamcham.domain.farming.EntryMode.MANUAL, false);
    private RecordFeedbackGenerationProcessor processor;

    @BeforeEach
    void setUp() throws Exception {
        processor = new RecordFeedbackGenerationProcessor(feedbackRepository, generationService, objectMapper, new NoOpTransactionManager());
    }

    @Test
    @DisplayName("processor stores validated content in typed fields and ordered action rows")
    void processor_stores_validated_content_in_typed_fields_and_ordered_action_rows() throws Exception {
        var feedback = pendingFeedback();
        var event = event(feedback);
        stubFeedback(event, feedback);
        when(generationService.generate(anyContext(), org.mockito.Mockito.isNull())).thenReturn(generationResult());
        processor.generate(event);
        assertThat(feedback.getStatus()).isEqualTo(RecordFeedbackStatus.READY);
        assertThat(feedback.getGoodPointText()).isEqualTo("가지 정리 시점이 적절합니다.");
        assertThat(feedback.nextActions().stream().map(it -> it.getText()).toList()).containsExactly("새순 상태를 확인하세요.", "이번 주 병해충 흔적을 확인하세요.");
        assertThat(feedback.getCitations()).containsExactly(map(new AbstractMap.SimpleEntry<>("id", "doc-1"), new AbstractMap.SimpleEntry<>("title", "농업기술길잡이")));
        assertThat(feedback.getAuditWarnings()).containsExactly("날씨 근거 없음");
    }

    @Test
    @DisplayName("processor stores expected generation failure")
    void processor_stores_expected_generation_failure() throws Exception {
        var feedback = pendingFeedback();
        var event = event(feedback);
        stubFeedback(event, feedback);
        when(generationService.generate(anyContext(), org.mockito.Mockito.isNull())).thenThrow(new RecordFeedbackGenerationFailure(RecordFeedbackFailureCode.INSUFFICIENT_EVIDENCE, null));
        processor.generate(event);
        assertThat(feedback.getStatus()).isEqualTo(RecordFeedbackStatus.FAILED);
        assertThat(feedback.getFailureCode()).isEqualTo("INSUFFICIENT_EVIDENCE");
    }

    @Test
    @DisplayName("processor logs safe validation diagnostics without raw generated content")
    void processor_logs_safe_validation_diagnostics_without_raw_generated_content(CapturedOutput output) throws Exception {
        var feedback = pendingFeedback();
        var event = event(feedback);
        stubFeedback(event, feedback);
        when(generationService.generate(anyContext(), org.mockito.Mockito.isNull())).thenThrow(new RecordFeedbackGenerationFailure(RecordFeedbackFailureCode.STRUCTURED_OUTPUT_INVALID, new IllegalStateException("invalid product output: next_action_1_text_length,next_action_1_text_english,"+
        "unknown_evidence:untrusted-generated-value")));
        processor.generate(event);
        var logs = output.getOut()+ output.getErr();
        assertThat(logs).contains("record feedback generation failed").contains("STRUCTURED_OUTPUT_INVALID").contains("next_action_1_text_length").contains("next_action_1_text_english").contains("unknown_evidence").doesNotContain("untrusted-generated-value");
    }

    @Test
    @DisplayName("processor preserves stale feedback when generation finishes late")
    void processor_preserves_stale_feedback_when_generation_finishes_late() throws Exception {
        var feedback = pendingFeedback();
        var event = event(feedback);
        stubFeedback(event, feedback);
        when(generationService.generate(anyContext(), org.mockito.Mockito.isNull())).thenAnswer(it -> {
        feedback.markStale();
        return generationResult();
        });
        assertDoesNotThrow(() -> processor.generate(event));
        assertThat(feedback.getStatus()).isEqualTo(RecordFeedbackStatus.STALE);
        assertThat(feedback.getGoodPointText()).isNull();
    }

    @Test
    @DisplayName("processor marks malformed snapshot as failed")
    void processor_marks_malformed_snapshot_as_failed() throws Exception {
        var feedback = new RecordFeedback(UUID.randomUUID(), member, record, RecordFeedbackStatus.PENDING, 3);
        feedback.attachInputSnapshot(map(new AbstractMap.SimpleEntry<>("schemaVersion", "invalid")));
        var event = event(feedback);
        stubFeedback(event, feedback);
        processor.generate(event);
        assertThat(feedback.getStatus()).isEqualTo(RecordFeedbackStatus.FAILED);
        assertThat(feedback.getFailureCode()).isEqualTo("INVALID_CONTEXT_SNAPSHOT");
    }
    private RecordFeedback pendingFeedback() {
        return also(new RecordFeedback(UUID.randomUUID(), member, record, RecordFeedbackStatus.PENDING, 3), it -> it.attachInputSnapshot(objectMapper.convertValue(context(), SNAPSHOT_TYPE)));
    }
    private RecordFeedbackGenerationRequested event(RecordFeedback feedback) {
        return new RecordFeedbackGenerationRequested(feedback.getId(), member.getId(), record.getId(), feedback.getSourceRevision());
    }
    private void stubFeedback(RecordFeedbackGenerationRequested event, RecordFeedback feedback) {
        when(feedbackRepository.findByIdAndMember_Id(event.getFeedbackId(), event.getMemberId())).thenReturn(feedback);
        when(feedbackRepository.findByIdAndMemberIdForUpdate(event.getFeedbackId(), event.getMemberId())).thenReturn(feedback);
    }
    private RecordFeedbackContext context() {
        return new RecordFeedbackContext(RecordFeedbackContext.RECORD_FEEDBACK_CONTEXT_SCHEMA_VERSION, new RecordFeedbackMemberContext(member.getId(), null, null), new RecordFeedbackFarmContext(farm.getId(), farm.getName(), farm.getRoadAddress(), null, null), new RecordFeedbackCropContext(crop.getId(), crop.getName(), crop.getUsePartCategory()), new RecordFeedbackRecordContext(record.getId(), SOURCE_REVISION, record.getWorkedAt(), record.getWorkType(), CommonFeedbackDetail.INSTANCE, record.getWeatherCondition(), record.getWeatherTemperature(), record.getMemo(), 0), null, List.of());
    }
    private RecordFeedbackGenerationResult generationResult() {
        return new RecordFeedbackGenerationResult(new RecordFeedbackContent(new RecordFeedbackGoodPoint("기록된 작업", "가지 정리 시점이 적절합니다.", java.util.Arrays.asList("doc-1")), java.util.Arrays.asList(new RecordFeedbackAction(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionCategory.CULTIVATION, "새순 확인", "새순 상태를 확인하세요.", java.util.Arrays.asList("doc-1")), new RecordFeedbackAction(RecordFeedbackActionDue.THIS_WEEK, RecordFeedbackActionCategory.PEST_DISEASE, "병해충 확인", "이번 주 병해충 흔적을 확인하세요.", java.util.Arrays.asList("doc-1")))), java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("id", "doc-1"), new AbstractMap.SimpleEntry<>("title", "농업기술길잡이"))), java.util.Arrays.asList("날씨 근거 없음"), new RagModelInfo("embedding-test", "chat-test"));
    }
    private RecordFeedbackContext anyContext() {
        return (org.mockito.Mockito.any(RecordFeedbackContext.class) != null ? org.mockito.Mockito.any(RecordFeedbackContext.class) : context());
    }
    private static final long SOURCE_REVISION = 3L;
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
