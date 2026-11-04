package com.chamchamcham.application.coaching.reportfeedback.lifecycle;

import com.chamchamcham.domain.report.WeedingStatistics;
import com.chamchamcham.domain.report.PlantingStatistics;
import com.chamchamcham.domain.report.FertilizingStatistics;
import com.chamchamcham.domain.report.HarvestStatistics;
import com.chamchamcham.domain.report.PestControlStatistics;
import com.chamchamcham.domain.report.CommonOnlyStatistics;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContext;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextAssembler;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextFingerprint;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackRecord;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackReport;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
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
import com.chamchamcham.domain.report.FarmingCycleReport;
import com.chamchamcham.domain.report.FarmingCycleReportProjection;
import com.chamchamcham.domain.report.FarmingCycleReportRepository;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import com.chamchamcham.domain.report.WateringStatistics;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

@ExtendWith({MockitoExtension.class})
class ReportFeedbackLifecycleServiceTest {
    private final UUID memberId = UUID.randomUUID();
    private final UUID reportId = UUID.randomUUID();
    private final UUID wateringFeedbackId = UUID.randomUUID();
    private final UUID harvestFeedbackId = UUID.randomUUID();
    private final String wateringFingerprint = "a".repeat(64);
    private final String latestWateringFingerprint = "b".repeat(64);
    private final String harvestFingerprint = "c".repeat(64);
    private Member member;
    private FarmingCycleReport report;
    @Mock private ReportFeedbackRepository feedbackRepository;
    @Mock private FarmingCycleReportRepository reportRepository;
    @Mock private ReportFeedbackContextAssembler contextAssembler;
    @Mock private ReportFeedbackContextFingerprint contextFingerprint;
    @Mock private ApplicationEventPublisher eventPublisher;
    private ReportFeedbackLifecycleService service;

    @BeforeEach
    void setUp() throws Exception {
        member = new Member(memberId, "member@example.com", null);
        var farm = new Farm(UUID.randomUUID(), member, "약초농장", "강원도 평창군");
        var crop = new Crop(UUID.randomUUID(), 422, "황기", CropUsePartCategory.ROOT_BARK);
        var finalHarvest = new FarmingRecord(UUID.randomUUID(), member, farm, crop, WorkType.HARVEST, LocalDateTime.of(2026, 7, 1, 9, 0), "맑음", 24, "최종 수확", com.chamchamcham.domain.farming.EntryMode.MANUAL, false);
        report = also(FarmingCycleReport.create(member, farm, crop, new FarmingCycleReportProjection(FarmingCycleReportStatus.COMPLETED, finalHarvest.getWorkedAt().minusMonths(5), finalHarvest.getWorkedAt(), FarmingCycleStartBasis.FIRST_RECORD, finalHarvest, 1, new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(1, null, null, 0, null, 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty()))), it -> setId(it, reportId));
        service = new ReportFeedbackLifecycleService(feedbackRepository, reportRepository, contextAssembler, contextFingerprint, eventPublisher);
    }

    @Test
    @DisplayName("first reconcile creates ordered pending feedback with current fingerprints and events")
    void first_reconcile_creates_ordered_pending_feedback_with_current_fingerprints_and_events() throws Exception {
        var wateringContext = context(WorkType.WATERING, 1, 1);
        var harvestContext = context(WorkType.HARVEST, 1, 1);
        when(feedbackRepository.findAllByReportAndMemberForUpdate(reportId, memberId)).thenReturn(List.of());
        when(contextAssembler.assemble(memberId, reportId, WorkType.WATERING)).thenReturn(wateringContext);
        when(contextAssembler.assemble(memberId, reportId, WorkType.HARVEST)).thenReturn(harvestContext);
        when(contextFingerprint.calculate(wateringContext)).thenReturn(wateringFingerprint);
        when(contextFingerprint.calculate(harvestContext)).thenReturn(harvestFingerprint);
        when(feedbackRepository.saveAll(Mockito.anyList())).thenAnswer(invocation -> {
        List<ReportFeedback> feedbacks = invocation.getArgument(0);
        setId(feedbacks.get(0), wateringFeedbackId);
        setId(feedbacks.get(1), harvestFeedbackId);
        return feedbacks;
        });
        var feedbacks = service.reconcile(report, Set.of(WorkType.HARVEST, WorkType.WATERING));
        assertThat(feedbacks.stream().map(ReportFeedback::getWorkType).toList()).containsExactly(WorkType.WATERING, WorkType.HARVEST);
        assertThat(feedbacks.stream().map(ReportFeedback::getStatus).toList()).containsOnly(ReportFeedbackStatus.PENDING);
        assertThat(feedbacks.stream().map(ReportFeedback::getSourceFingerprint).toList()).containsExactly(wateringFingerprint, harvestFingerprint);
        var events = ArgumentCaptor.forClass(ReportFeedbackPreparationRequested.class);
        verify(eventPublisher, times(2)).publishEvent(events.capture());
        assertThat(events.getAllValues().stream().map(ReportFeedbackPreparationRequested::getSourceFingerprint).toList()).containsExactly(wateringFingerprint, harvestFingerprint);
        verify(feedbackRepository).findAllByReportAndMemberForUpdate(reportId, memberId);
        verify(feedbackRepository, never()).findAllByReport_IdAndMember_Id(reportId, memberId);
    }

    @Test
    @DisplayName("ready feedback becomes stale when its current context changes without publishing an event")
    void ready_feedback_becomes_stale_when_its_current_context_changes_without_publishing_an_event() throws Exception {
        var ready = readyFeedback(WorkType.WATERING, wateringFeedbackId, wateringFingerprint);
        var latestContext = context(WorkType.WATERING, 2, 1);
        when(feedbackRepository.findAllByReportAndMemberForUpdate(reportId, memberId)).thenReturn(java.util.Arrays.asList(ready));
        when(contextAssembler.assemble(memberId, reportId, WorkType.WATERING)).thenReturn(latestContext);
        when(contextFingerprint.calculate(latestContext)).thenReturn(latestWateringFingerprint);
        when(feedbackRepository.saveAll(Mockito.anyList())).thenAnswer(it -> it.getArgument(0));
        service.reconcile(report, Set.of(WorkType.WATERING));
        assertThat(ready.getStatus()).isEqualTo(ReportFeedbackStatus.STALE);
        assertThat(ready.getSourceFingerprint()).isEqualTo(wateringFingerprint);
        assertThat(ready.getInputSnapshot()).isNull();
        verify(eventPublisher, never()).publishEvent(Mockito.any());
    }

    @Test
    @DisplayName("unchanged legacy ready feedback backfills its snapshot fingerprint without changing status")
    void unchanged_legacy_ready_feedback_backfills_its_snapshot_fingerprint_without_changing_status() throws Exception {
        var snapshot = map(new AbstractMap.SimpleEntry<>("report", map(new AbstractMap.SimpleEntry<>("sourceRevision", 1), new AbstractMap.SimpleEntry<>("statistics", map(new AbstractMap.SimpleEntry<>("recordCount", 1))))), new AbstractMap.SimpleEntry<>("records", java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("id", "watering-1")))));
        var legacyReady = legacyReadyFeedback(snapshot);
        var currentContext = context(WorkType.WATERING, 1, 1);
        when(feedbackRepository.findAllByReportAndMemberForUpdate(reportId, memberId)).thenReturn(java.util.Arrays.asList(legacyReady));
        when(contextAssembler.assemble(memberId, reportId, WorkType.WATERING)).thenReturn(currentContext);
        when(contextFingerprint.calculate(currentContext)).thenReturn(wateringFingerprint);
        when(contextFingerprint.calculate(snapshot)).thenReturn(wateringFingerprint);
        when(feedbackRepository.saveAll(Mockito.anyList())).thenAnswer(it -> it.getArgument(0));
        service.reconcile(report, Set.of(WorkType.WATERING));
        assertThat(legacyReady.getStatus()).isEqualTo(ReportFeedbackStatus.READY);
        assertThat(legacyReady.getSourceFingerprint()).isEqualTo(wateringFingerprint);
        assertThat(legacyReady.getInputSnapshot()).isEqualTo(snapshot);
        verify(eventPublisher, never()).publishEvent(Mockito.any());
    }

    @Test
    @DisplayName("legacy ready feedback survives numeric normalization in its persisted snapshot")
    void legacy_ready_feedback_survives_numeric_normalization_in_its_persisted_snapshot() throws Exception {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        var actualFingerprint = new ReportFeedbackContextFingerprint(objectMapper);
        var currentContext = context(WorkType.WATERING, 1, new BigDecimal("1E+3"));
        Map<String, Object> snapshotBeforePersistence = objectMapper.convertValue(currentContext, SNAPSHOT_TYPE);
        Map<String, Object> persistedSnapshot = objectMapper.readValue(objectMapper.writeValueAsBytes(snapshotBeforePersistence), SNAPSHOT_TYPE);
        var legacyReady = legacyReadyFeedback(persistedSnapshot);
        when(feedbackRepository.findAllByReportAndMemberForUpdate(reportId, memberId)).thenReturn(java.util.Arrays.asList(legacyReady));
        when(contextAssembler.assemble(memberId, reportId, WorkType.WATERING)).thenReturn(currentContext);
        when(feedbackRepository.saveAll(Mockito.anyList())).thenAnswer(it -> it.getArgument(0));
        var numericService = new ReportFeedbackLifecycleService(feedbackRepository, reportRepository, contextAssembler, actualFingerprint, eventPublisher);
        numericService.reconcile(report, Set.of(WorkType.WATERING));
        assertThat(legacyReady.getStatus()).isEqualTo(ReportFeedbackStatus.READY);
        assertThat(legacyReady.getSourceFingerprint()).isEqualTo(actualFingerprint.calculate(currentContext));
        assertThat(legacyReady.getInputSnapshot()).isEqualTo(persistedSnapshot);
        verify(eventPublisher, never()).publishEvent(Mockito.any());
    }

    @Test
    @DisplayName("legacy feedback becomes stale when its snapshot fingerprint differs from current context")
    void legacy_feedback_becomes_stale_when_its_snapshot_fingerprint_differs_from_current_context() throws Exception {
        var snapshot = map(new AbstractMap.SimpleEntry<>("report", map(new AbstractMap.SimpleEntry<>("sourceRevision", 1))));
        var legacyReady = legacyReadyFeedback(snapshot);
        var currentContext = context(WorkType.WATERING, 2, 1);
        when(feedbackRepository.findAllByReportAndMemberForUpdate(reportId, memberId)).thenReturn(java.util.Arrays.asList(legacyReady));
        when(contextAssembler.assemble(memberId, reportId, WorkType.WATERING)).thenReturn(currentContext);
        when(contextFingerprint.calculate(currentContext)).thenReturn(latestWateringFingerprint);
        when(contextFingerprint.calculate(snapshot)).thenReturn(wateringFingerprint);
        when(feedbackRepository.saveAll(Mockito.anyList())).thenAnswer(it -> it.getArgument(0));
        service.reconcile(report, Set.of(WorkType.WATERING));
        assertThat(legacyReady.getStatus()).isEqualTo(ReportFeedbackStatus.STALE);
        assertThat(legacyReady.getSourceFingerprint()).isNull();
        assertThat(legacyReady.getInputSnapshot()).isNull();
    }

    @Test
    @DisplayName("legacy feedback without an input snapshot becomes stale")
    void legacy_feedback_without_an_input_snapshot_becomes_stale() throws Exception {
        var legacyReady = legacyReadyFeedback(null);
        var currentContext = context(WorkType.WATERING, 1, 1);
        when(feedbackRepository.findAllByReportAndMemberForUpdate(reportId, memberId)).thenReturn(java.util.Arrays.asList(legacyReady));
        when(contextAssembler.assemble(memberId, reportId, WorkType.WATERING)).thenReturn(currentContext);
        when(contextFingerprint.calculate(currentContext)).thenReturn(wateringFingerprint);
        when(feedbackRepository.saveAll(Mockito.anyList())).thenAnswer(it -> it.getArgument(0));
        service.reconcile(report, Set.of(WorkType.WATERING));
        assertThat(legacyReady.getStatus()).isEqualTo(ReportFeedbackStatus.STALE);
    }

    @Test
    @DisplayName("legacy feedback with an unusable input snapshot becomes stale")
    void legacy_feedback_with_an_unusable_input_snapshot_becomes_stale() throws Exception {
        var snapshot = map(new AbstractMap.SimpleEntry<>("unsupported", new Object()));
        var legacyReady = legacyReadyFeedback(snapshot);
        var currentContext = context(WorkType.WATERING, 1, 1);
        when(feedbackRepository.findAllByReportAndMemberForUpdate(reportId, memberId)).thenReturn(java.util.Arrays.asList(legacyReady));
        when(contextAssembler.assemble(memberId, reportId, WorkType.WATERING)).thenReturn(currentContext);
        when(contextFingerprint.calculate(currentContext)).thenReturn(wateringFingerprint);
        when(contextFingerprint.calculate(snapshot)).thenThrow(new IllegalArgumentException("unsupported snapshot"));
        when(feedbackRepository.saveAll(Mockito.anyList())).thenAnswer(it -> it.getArgument(0));
        service.reconcile(report, Set.of(WorkType.WATERING));
        assertThat(legacyReady.getStatus()).isEqualTo(ReportFeedbackStatus.STALE);
    }

    @Test
    @DisplayName("new work type becomes a stale placeholder after feedback already exists")
    void new_work_type_becomes_a_stale_placeholder_after_feedback_already_exists() throws Exception {
        var ready = readyFeedback(WorkType.WATERING, wateringFeedbackId, wateringFingerprint);
        var currentContext = context(WorkType.WATERING, 1, 1);
        when(feedbackRepository.findAllByReportAndMemberForUpdate(reportId, memberId)).thenReturn(java.util.Arrays.asList(ready));
        when(contextAssembler.assemble(memberId, reportId, WorkType.WATERING)).thenReturn(currentContext);
        when(contextFingerprint.calculate(currentContext)).thenReturn(wateringFingerprint);
        when(feedbackRepository.saveAll(Mockito.anyList())).thenAnswer(it -> it.getArgument(0));
        var reconciled = service.reconcile(report, Set.of(WorkType.WATERING, WorkType.FERTILIZING));
        var placeholder = reconciled.stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.FERTILIZING)).findFirst().orElseThrow();
        assertThat(placeholder.getStatus()).isEqualTo(ReportFeedbackStatus.STALE);
        assertThat(placeholder.getSourceFingerprint()).isNull();
        verify(eventPublisher, never()).publishEvent(Mockito.any());
    }

    @Test
    @DisplayName("removed work type becomes stale and remains stored without an event")
    void removed_work_type_becomes_stale_and_remains_stored_without_an_event() throws Exception {
        var watering = readyFeedback(WorkType.WATERING, wateringFeedbackId, wateringFingerprint);
        var harvest = readyFeedback(WorkType.HARVEST, harvestFeedbackId, harvestFingerprint);
        var currentContext = context(WorkType.WATERING, 1, 1);
        when(feedbackRepository.findAllByReportAndMemberForUpdate(reportId, memberId)).thenReturn(java.util.Arrays.asList(harvest, watering));
        when(contextAssembler.assemble(memberId, reportId, WorkType.WATERING)).thenReturn(currentContext);
        when(contextFingerprint.calculate(currentContext)).thenReturn(wateringFingerprint);
        when(feedbackRepository.saveAll(Mockito.anyList())).thenAnswer(it -> it.getArgument(0));
        var reconciled = service.reconcile(report, Set.of(WorkType.WATERING));
        assertThat(reconciled).contains(harvest);
        assertThat(harvest.getStatus()).isEqualTo(ReportFeedbackStatus.STALE);
        verify(eventPublisher, never()).publishEvent(Mockito.any());
    }

    @Test
    @DisplayName("manual regeneration locks stale feedback and publishes preparation with the latest fingerprint")
    void manual_regeneration_locks_stale_feedback_and_publishes_preparation_with_the_latest_fingerprint() throws Exception {
        var stale = also(ReportFeedback.stalePlaceholder(member, report, WorkType.WATERING), it -> setId(it, wateringFeedbackId));
        var latestContext = context(WorkType.WATERING, 2, 1);
        when(reportRepository.findByIdAndMemberIdForUpdate(reportId, memberId)).thenReturn(report);
        when(feedbackRepository.findByReportAndWorkTypeForUpdate(reportId, memberId, WorkType.WATERING)).thenReturn(stale);
        when(contextAssembler.assemble(memberId, report, WorkType.WATERING)).thenReturn(latestContext);
        when(contextFingerprint.calculate(latestContext)).thenReturn(latestWateringFingerprint);
        var regenerated = service.regenerate(memberId, reportId, WorkType.WATERING);
        assertThat(regenerated.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(regenerated.getSourceFingerprint()).isEqualTo(latestWateringFingerprint);
        assertThat(regenerated.getInputSnapshot()).isNull();
        var event = new ReportFeedbackPreparationRequested(wateringFeedbackId, memberId, reportId, WorkType.WATERING, latestWateringFingerprint);
        var order = Mockito.inOrder(reportRepository, feedbackRepository, contextAssembler, eventPublisher);
        order.verify(reportRepository).findByIdAndMemberIdForUpdate(reportId, memberId);
        order.verify(feedbackRepository).findByReportAndWorkTypeForUpdate(reportId, memberId, WorkType.WATERING);
        order.verify(contextAssembler).assemble(memberId, report, WorkType.WATERING);
        order.verify(eventPublisher).publishEvent(event);
    }

    @Test
    @DisplayName("manual regeneration rejects ready feedback before assembling context")
    void manual_regeneration_rejects_ready_feedback_before_assembling_context() throws Exception {
        var ready = readyFeedback(WorkType.WATERING, wateringFeedbackId, wateringFingerprint);
        when(reportRepository.findByIdAndMemberIdForUpdate(reportId, memberId)).thenReturn(report);
        when(feedbackRepository.findByReportAndWorkTypeForUpdate(reportId, memberId, WorkType.WATERING)).thenReturn(ready);
        assertThatThrownBy(() -> service.regenerate(memberId, reportId, WorkType.WATERING)).isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.REPORT_FEEDBACK_REGENERATION_NOT_ALLOWED);
        verifyNoInteractions(contextAssembler);
        verify(eventPublisher, never()).publishEvent(Mockito.any());
    }

    @Test
    @DisplayName("manual regeneration rejects a report outside member ownership before locking feedback")
    void manual_regeneration_rejects_a_report_outside_member_ownership_before_locking_feedback() throws Exception {
        when(reportRepository.findByIdAndMemberIdForUpdate(reportId, memberId)).thenReturn(null);
        assertThatThrownBy(() -> service.regenerate(memberId, reportId, WorkType.WATERING)).isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.REPORT_NOT_FOUND);
        verifyNoInteractions(feedbackRepository, contextAssembler);
    }

    @Test
    @DisplayName("manual regeneration rejects a non-completed locked report before locking feedback")
    void manual_regeneration_rejects_a_non_completed_locked_report_before_locking_feedback() throws Exception {
        report.supersede();
        when(reportRepository.findByIdAndMemberIdForUpdate(reportId, memberId)).thenReturn(report);
        assertThatThrownBy(() -> service.regenerate(memberId, reportId, WorkType.WATERING)).isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.REPORT_NOT_FOUND);
        verifyNoInteractions(feedbackRepository, contextAssembler);
    }

    @Test
    @DisplayName("manual regeneration rejects a work type absent from locked report statistics")
    void manual_regeneration_rejects_a_work_type_absent_from_locked_report_statistics() throws Exception {
        setStatistics(report, CycleReportStatistics.empty());
        when(reportRepository.findByIdAndMemberIdForUpdate(reportId, memberId)).thenReturn(report);
        assertThatThrownBy(() -> service.regenerate(memberId, reportId, WorkType.WATERING)).isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.REPORT_FEEDBACK_NOT_FOUND);
        verifyNoInteractions(feedbackRepository, contextAssembler);
    }
    private ReportFeedback readyFeedback(WorkType workType, UUID id, String sourceFingerprint) {
        return also(ReportFeedback.pending(member, report, workType, sourceFingerprint), it -> {
        setId(it, id);
        it.attachInputSnapshot(map(new AbstractMap.SimpleEntry<>("sourceRevision", 1)));
        it.markReady("" + workType.getLabel() + " 요약", List.of(), List.of(), "PASS", List.of(), "test-chat", "test-embedding");
        });
    }
    private ReportFeedback legacyReadyFeedback(Map<String, Object> snapshot) {
        return also(new ReportFeedback(wateringFeedbackId, member, report, WorkType.WATERING, ReportFeedbackStatus.PENDING), feedback -> {
        if (snapshot != null) feedback.attachInputSnapshot(snapshot);
        feedback.markReady("관수 작업 요약", List.of(), List.of(), "PASS", List.of(), "test-chat", "test-embedding");
        });
    }
    private ReportFeedbackContext context(WorkType workType, long sourceRevision, Object recordCount) {
        return new ReportFeedbackContext(ReportFeedbackContextKt.REPORT_FEEDBACK_CONTEXT_SCHEMA_VERSION, workType, new ReportFeedbackReport(reportId, report.getFarm().getName(), report.getCrop().getName(), report.getStartsAt(), Objects.requireNonNull(report.getEndsAt()), sourceRevision, map(new AbstractMap.SimpleEntry<>("recordCount", recordCount))), java.util.Arrays.asList(new ReportFeedbackRecord(new UUID(0, (long) workType.ordinal()+ 1), report.getStartsAt().plusDays(1), workType, "" + workType.getLabel() + " 기록", Map.of())), null, List.of(), List.of());
    }
    private void setId(Object target, UUID id) { org.springframework.test.util.ReflectionTestUtils.setField(target, "id", id); }
    private void setStatistics(FarmingCycleReport target, CycleReportStatistics statistics) { org.springframework.test.util.ReflectionTestUtils.setField(target, "statistics", statistics); }
    private static final TypeReference<Map<String, Object>> SNAPSHOT_TYPE = new TypeReference<>() {};
    @SafeVarargs
    private static <K> Map<K, Object> map(Map.Entry<K, ?>... entries) {
        Map<K, Object> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private static <T> T also(T value, java.util.function.Consumer<T> action) { action.accept(value); return value; }

}
