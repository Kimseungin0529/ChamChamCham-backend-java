package com.chamchamcham.api.coaching.recordfeedback;

import com.chamchamcham.ApiApplication;
import com.chamchamcham.application.coaching.recordfeedback.lifecycle.*;
import com.chamchamcham.application.coaching.common.RagModelInfo;
import com.chamchamcham.application.coaching.recordfeedback.generation.*;
import com.chamchamcham.application.farming.FarmingRecordCommand;
import com.chamchamcham.application.farming.FarmingRecordService;
import com.chamchamcham.application.report.FarmingCycleReportProjectionService;
import com.chamchamcham.domain.coaching.recordfeedback.*;
import com.chamchamcham.domain.crop.*;
import com.chamchamcham.domain.farm.*;
import com.chamchamcham.domain.farming.*;
import com.chamchamcham.domain.member.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = ApiApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:record-feedback-lifecycle;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON"
})
@ActiveProfiles("test")
class RecordFeedbackLifecycleIntegrationTest {
    @Autowired private FarmingRecordService farmingRecordService;
    @Autowired private FarmingRecordRepository farmingRecordRepository;
    @Autowired private WateringRecordRepository wateringRecordRepository;
    @Autowired private RecordFeedbackRepository recordFeedbackRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberCropRepository memberCropRepository;
    @Autowired private FarmRepository farmRepository;
    @Autowired private CropRepository cropRepository;
    @Autowired private RecordFeedbackGenerationProcessor generationProcessor;
    @Autowired private RecordFeedbackQueryService queryService;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockBean private FarmingCycleReportProjectionService projectionService;
    @MockBean private RecordFeedbackGenerationService generationService;
    private Member member;
    private Farm farm;
    private Crop crop;

    @BeforeEach
    void setUp() {
        recordFeedbackRepository.deleteAll();
        wateringRecordRepository.deleteAllInBatch();
        farmingRecordRepository.deleteAllInBatch();
        memberCropRepository.deleteAllInBatch();
        farmRepository.deleteAllInBatch();
        cropRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
        member = memberRepository.save(new Member("feedback-lifecycle@example.com", null));
        farm = farmRepository.save(new Farm(member, "코칭 검증 밭", "강원도 평창군"));
        crop = cropRepository.save(new Crop(7101, "황기", CropUsePartCategory.ROOT_BARK));
        memberCropRepository.save(new MemberCrop(member, farm, crop));
        when(generationService.generate(any(RecordFeedbackContext.class), isNull())).thenReturn(generatedFeedback());
    }

    @Test
    @DisplayName("create commits a ready feedback with an entity derived input snapshot")
    void createCommitsAReadyFeedbackWithAnEntityDerivedInputSnapshot() throws Exception {
        var recordId = farmingRecordService.create(wateringCreateCommand()).getId();
        var feedback = awaitReadyFeedback(recordId, 1);
        assertThat(feedback.getSourceRevision()).isEqualTo(1);
        assertThat(feedback.getStatus()).isEqualTo(RecordFeedbackStatus.READY);
        assertThat(feedback.getInputSnapshot()).containsEntry("schemaVersion", "record-feedback-context.v2")
                .doesNotContainKeys("recentRecords", "workTypeStats", "cropCycle", "daysAfterPlanting");
        assertThat(feedback.getGoodPointText()).isEqualTo("관수 기록이 구체적입니다.");
        assertThat(feedback.getAuditStatus()).isEqualTo("PASS");
        assertThat(feedback.getModelName()).isEqualTo("chat-test");
    }

    @Test
    @DisplayName("automatic record feedback keeps one immutable snapshot and exposes only user output")
    @SuppressWarnings("unchecked")
    void automaticRecordFeedbackKeepsOneImmutableSnapshotAndExposesOnlyUserOutput() throws Exception {
        var recordId = farmingRecordService.create(wateringCreateCommand()).getId();
        var feedback = awaitReadyFeedback(recordId, 1);
        var feedbackId = Objects.requireNonNull(feedback.getId());
        var snapshot = Objects.requireNonNull(feedback.getInputSnapshot());
        var statusResult = queryService.get(Objects.requireNonNull(member.getId()), recordId);
        assertThat(feedback.getStatus()).isEqualTo(RecordFeedbackStatus.READY);
        assertThat(snapshot).containsEntry("schemaVersion", "record-feedback-context.v2");
        var recordSnapshot = (Map<String, Object>) snapshot.get("record");
        assertThat(recordSnapshot).containsEntry("recordId", recordId.toString())
                .containsEntry("sourceRevision", 1).containsEntry("workType", "WATERING")
                .containsEntry("memo", "점적관수로 토양 수분을 보충했습니다.");
        assertThat(statusResult.getContent() == null ? null : statusResult.getContent().getGoodPoint())
                .isEqualTo("관수 기록이 구체적입니다.");
        var nextActions = statusResult.getContent() == null ? List.<RecordFeedbackNextActionResult>of()
                : statusResult.getContent().getNextActions();
        assertThat(nextActions).hasSize(2);
        assertThat(nextActions).extracting(RecordFeedbackNextActionResult::getText)
                .containsExactly("토양 수분을 다시 확인하세요.", "관수량 변화를 주간 기록에 남기세요.");
        assertThat(nextActions).extracting(RecordFeedbackNextActionResult::getDue)
                .containsExactly(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionDue.THIS_WEEK);
        assertThat(nextActions).extracting(RecordFeedbackNextActionResult::getCategory)
                .containsExactly(RecordFeedbackActionCategory.CULTIVATION, RecordFeedbackActionCategory.IRRIGATION);
        generationProcessor.generate(new RecordFeedbackGenerationRequested(feedbackId,
                Objects.requireNonNull(member.getId()), recordId, 1));
        var feedbacks = recordFeedbackRepository.findAll().stream().filter(it -> it.getRecord().getId().equals(recordId)).toList();
        var reloaded = recordFeedbackRepository.findById(feedbackId).orElseThrow();
        assertThat(feedbacks).hasSize(1);
        assertThat(feedbacks.getFirst().getId()).isEqualTo(feedbackId);
        assertThat(reloaded.getStatus()).isEqualTo(RecordFeedbackStatus.READY);
        assertThat(reloaded.getInputSnapshot()).isEqualTo(snapshot);
        verify(generationService, times(1)).generate(any(RecordFeedbackContext.class), isNull());
    }

    @Test
    @DisplayName("update stales the prior revision and prepares a new ready snapshot")
    void updateStalesThePriorRevisionAndPreparesANewReadySnapshot() throws Exception {
        var recordId = farmingRecordService.create(wateringCreateCommand()).getId();
        awaitReadyFeedback(recordId, 1);
        farmingRecordService.update(wateringUpdateCommand(recordId));
        var second = awaitReadyFeedback(recordId, 2);
        var first = Objects.requireNonNull(recordFeedbackRepository.findByRecord_IdAndSourceRevision(recordId, 1),
                "first revision feedback must exist");
        assertThat(second.getSourceRevision()).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(RecordFeedbackStatus.STALE);
        assertThat(second.getStatus()).isEqualTo(RecordFeedbackStatus.READY);
        assertThat(second.getInputSnapshot()).containsEntry("schemaVersion", "record-feedback-context.v2");
        assertThat(second.getGoodPointText()).isEqualTo("관수 기록이 구체적입니다.");
    }

    @Test
    @DisplayName("delete stales active feedback without creating a replacement")
    void deleteStalesActiveFeedbackWithoutCreatingAReplacement() throws Exception {
        var recordId = farmingRecordService.create(wateringCreateCommand()).getId();
        awaitReadyFeedback(recordId, 1);
        farmingRecordService.delete(new FarmingRecordCommand.Delete(Objects.requireNonNull(member.getId()), recordId));
        var record = farmingRecordRepository.findById(recordId).orElseThrow();
        var feedback = Objects.requireNonNull(recordFeedbackRepository.findByRecord_IdAndSourceRevision(recordId, 1),
                "initial feedback must exist");
        assertThat(record.isDeleted()).isTrue();
        assertThat(feedback.getStatus()).isEqualTo(RecordFeedbackStatus.STALE);
        assertThat(recordFeedbackRepository.findByRecord_IdAndSourceRevision(recordId, 2)).isNull();
    }

    @Test
    @DisplayName("concurrent first enqueue allocates distinct feedback revisions")
    void concurrentFirstEnqueueAllocatesDistinctFeedbackRevisions() throws Exception {
        var record = farmingRecordRepository.saveAndFlush(new FarmingRecord(null, member, farm, crop,
                WorkType.WATERING, LocalDateTime.of(2026, 7, 13, 8, 30), "맑음", 23,
                "동시 코칭 생성 검증 기록입니다.", EntryMode.MANUAL, false));
        var recordId = Objects.requireNonNull(record.getId());
        var lifecycleService = new RecordFeedbackLifecycleService(recordFeedbackRepository, (ApplicationEventPublisher) event -> { });
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var futures = new ArrayList<Future<Long>>();
            for (int i = 0; i < 2; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("concurrent enqueue start timed out");
                    return Objects.requireNonNull(new TransactionTemplate(transactionManager).execute(status -> {
                        var currentRecord = farmingRecordRepository.findById(recordId).orElseThrow();
                        return lifecycleService.enqueue(currentRecord).getSourceRevision();
                    }));
                }));
            }
            if (!ready.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("concurrent enqueue workers did not become ready");
            start.countDown();
            var revisions = new ArrayList<Long>();
            for (var future : futures) revisions.add(future.get(5, TimeUnit.SECONDS));
            assertThat(revisions).containsExactlyInAnyOrder(1L, 2L);
            assertThat(recordFeedbackRepository.findAll().stream().filter(it -> it.getRecord().getId().equals(recordId))
                    .map(RecordFeedback::getSourceRevision)).containsExactlyInAnyOrder(1L, 2L);
        } finally { executor.shutdownNow(); }
    }

    private RecordFeedback awaitReadyFeedback(UUID recordId, long revision) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        RecordFeedbackStatus lastStatus = null;
        while (System.nanoTime() < deadline) {
            var feedback = recordFeedbackRepository.findByRecord_IdAndSourceRevision(recordId, revision);
            lastStatus = feedback == null ? null : feedback.getStatus();
            if (feedback != null && feedback.getStatus() == RecordFeedbackStatus.READY) return feedback;
            if (feedback != null && feedback.getStatus() == RecordFeedbackStatus.FAILED) {
                throw new IllegalStateException("record feedback failed: recordId=" + recordId + " revision=" + revision + " code=" + feedback.getFailureCode());
            }
            Thread.sleep(25);
        }
        throw new IllegalStateException("record feedback did not become READY: recordId=" + recordId + " revision=" + revision + " lastStatus=" + lastStatus);
    }

    private FarmingRecordCommand.Create wateringCreateCommand() {
        return new FarmingRecordCommand.Create(Objects.requireNonNull(member.getId()), Objects.requireNonNull(farm.getId()),
                Objects.requireNonNull(crop.getId()), WorkType.WATERING, LocalDateTime.of(2026, 7, 11, 8, 30),
                "맑음", 24, "점적관수로 토양 수분을 보충했습니다.", null,
                new FarmingRecordCommand.WateringDetail(IrrigationAmount.NORMAL, IrrigationMethod.DRIP),
                null, null, null, null, List.of(), EntryMode.MANUAL);
    }

    private FarmingRecordCommand.Update wateringUpdateCommand(UUID recordId) {
        return new FarmingRecordCommand.Update(Objects.requireNonNull(member.getId()), recordId, Objects.requireNonNull(farm.getId()),
                Objects.requireNonNull(crop.getId()), WorkType.WATERING, LocalDateTime.of(2026, 7, 12, 8, 30),
                "흐림", 22, "전날 강우를 반영해 관수량을 조절했습니다.", null,
                new FarmingRecordCommand.WateringDetail(IrrigationAmount.LOW, IrrigationMethod.DRIP),
                null, null, null, null, List.of());
    }

    private RecordFeedbackGenerationResult generatedFeedback() {
        return new RecordFeedbackGenerationResult(new RecordFeedbackContent(
                new RecordFeedbackGoodPoint("자동 생성 테스트 근거입니다.", "관수 기록이 구체적입니다.", List.of("doc-1")), List.of(
                        new RecordFeedbackAction(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionCategory.CULTIVATION,
                                "다음 점검이 필요합니다.", "토양 수분을 다시 확인하세요.", List.of("doc-1")),
                        new RecordFeedbackAction(RecordFeedbackActionDue.THIS_WEEK, RecordFeedbackActionCategory.IRRIGATION,
                                "주간 관수량 추적이 필요합니다.", "관수량 변화를 주간 기록에 남기세요.", List.of("doc-1")))),
                List.of(Map.of("id", "doc-1", "title", "농업기술길잡이")), List.of(), new RagModelInfo("embedding-test", "chat-test"));
    }
}
