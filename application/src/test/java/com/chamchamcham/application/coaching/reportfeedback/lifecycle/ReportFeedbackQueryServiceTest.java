package com.chamchamcham.application.coaching.reportfeedback.lifecycle;

import com.chamchamcham.domain.report.WeedingStatistics;
import com.chamchamcham.domain.report.PlantingStatistics;
import com.chamchamcham.domain.report.PestControlStatistics;
import com.chamchamcham.domain.report.Coverage;
import com.chamchamcham.domain.report.CommonOnlyStatistics;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedback;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackItemDraft;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackItemSection;
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
import com.chamchamcham.domain.report.FertilizingStatistics;
import com.chamchamcham.domain.report.HarvestStatistics;
import com.chamchamcham.domain.report.WateringStatistics;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextKt;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

@ExtendWith({MockitoExtension.class})
class ReportFeedbackQueryServiceTest {
    private final UUID memberId = uuid("000000000001");
    private final UUID reportId = uuid("000000000101");
    private final UUID wateringFeedbackId = uuid("000000000201");
    private final UUID fertilizingFeedbackId = uuid("000000000202");
    private final UUID harvestFeedbackId = uuid("000000000203");
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 7, 14, 10, 0);
    private final LocalDateTime updatedAt = LocalDateTime.of(2026, 7, 14, 10, 1);
    @Mock private FarmingCycleReportRepository reportRepository;
    @Mock private ReportFeedbackRepository feedbackRepository;
    @Mock private ReportFeedbackLifecycleService lifecycleService;
    private ReportFeedbackQueryService service;
    private Member member;
    private FarmingCycleReport completedReport;

    @BeforeEach
    void setUp() throws Exception {
        service = new ReportFeedbackQueryService(reportRepository, feedbackRepository, lifecycleService);
        member = new Member(memberId, "member@example.com", null);
        completedReport = report(FarmingCycleReportStatus.COMPLETED, statisticsFor(WorkType.WATERING, WorkType.FERTILIZING, WorkType.HARVEST));
    }

    @Test
    @DisplayName("completed report returns work type feedbacks by latest work date with ready content only")
    void completed_report_returns_work_type_feedbacks_by_latest_work_date_with_ready_content_only() throws Exception {
        var watering = readyFeedback();
        var fertilizing = pendingFeedback();
        var harvest = failedFeedback();
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(completedReport);
        when(feedbackRepository.findAllByReport_IdAndMember_Id(reportId, memberId)).thenReturn(java.util.Arrays.asList(harvest, watering, fertilizing));
        var result = service.get(memberId, reportId);
        assertThat(result.getReportId()).isEqualTo(reportId);
        assertThat(result.getFeedbacks().stream().map(it -> it.getWorkType()).toList()).containsExactly(WorkType.HARVEST, WorkType.WATERING, WorkType.FERTILIZING);
        var ready = result.getFeedbacks().stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.WATERING)).findFirst().orElseThrow();
        assertThat(ready.getFeedbackId()).isEqualTo(wateringFeedbackId);
        assertThat(ready.getStatus()).isEqualTo(ReportFeedbackStatus.READY);
        assertThat(ready.getInputPrepared()).isTrue();
        assertThat(ready.getFailureCode()).isNull();
        assertThat(ready.getContent().getSummary()).isEqualTo("관수 작업 요약");
        assertThat(ready.getContent().getComparisons().stream().map(it -> it.getText()).toList()).containsExactly("직전 재배보다 물 주기 기록이 한 번 늘었어요.");
        assertThat(ready.getContent().getStrengths().stream().map(it -> it.getText()).toList()).containsExactly("수분 상태를 꾸준히 확인했어요.");
        assertThat(ready.getContent().getImprovements().stream().map(it -> it.getText()).toList()).containsExactly("관수 간격도 함께 비교해 보세요.");
        assertThat(ready.getContent().getNextActions().stream().map(it -> it.getText()).toList()).containsExactly("내일 토양 수분을 다시 확인하세요.");
        assertThat(ready.getCreatedAt()).isEqualTo(createdAt);
        assertThat(ready.getUpdatedAt()).isEqualTo(updatedAt);
        var pending = result.getFeedbacks().stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.FERTILIZING)).findFirst().orElseThrow();
        assertThat(pending.getFeedbackId()).isEqualTo(fertilizingFeedbackId);
        assertThat(pending.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(pending.getInputPrepared()).isFalse();
        assertThat(pending.getFailureCode()).isNull();
        assertThat(pending.getContent()).isNull();
        var failed = result.getFeedbacks().stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.HARVEST)).findFirst().orElseThrow();
        assertThat(failed.getFeedbackId()).isEqualTo(harvestFeedbackId);
        assertThat(failed.getStatus()).isEqualTo(ReportFeedbackStatus.FAILED);
        assertThat(failed.getInputPrepared()).isTrue();
        assertThat(failed.getFailureCode()).isEqualTo("STRUCTURED_OUTPUT_INVALID");
        assertThat(failed.getContent()).isNull();
    }

    @Test
    @DisplayName("completed report without feedback rows returns an empty collection")
    void completed_report_without_feedback_rows_returns_an_empty_collection() throws Exception {
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(completedReport);
        when(feedbackRepository.findAllByReport_IdAndMember_Id(reportId, memberId)).thenReturn(List.of());
        var result = service.get(memberId, reportId);
        assertThat(result.getReportId()).isEqualTo(reportId);
        assertThat(result.getFeedbacks()).isEmpty();
    }

    @Test
    @DisplayName("stale feedback hides persisted content and preparation details")
    void stale_feedback_hides_persisted_content_and_preparation_details() throws Exception {
        var stale = also(readyFeedback(), ReportFeedback::markStale);
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(completedReport);
        when(feedbackRepository.findAllByReport_IdAndMember_Id(reportId, memberId)).thenReturn(java.util.Arrays.asList(stale));
        var result = service.get(memberId, reportId).getFeedbacks().get(0);
        assertThat(result.getStatus()).isEqualTo(ReportFeedbackStatus.STALE);
        assertThat(result.getInputPrepared()).isFalse();
        assertThat(result.getFailureCode()).isNull();
        assertThat(result.getContent()).isNull();
    }

    @Test
    @DisplayName("feedback for a removed work type is excluded from the current report response")
    void feedback_for_a_removed_work_type_is_excluded_from_the_current_report_response() throws Exception {
        var wateringOnlyReport = report(FarmingCycleReportStatus.COMPLETED, statisticsFor(WorkType.WATERING));
        var watering = readyFeedback();
        var removedHarvest = also(failedFeedback(), ReportFeedback::markStale);
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(wateringOnlyReport);
        when(feedbackRepository.findAllByReport_IdAndMember_Id(reportId, memberId)).thenReturn(java.util.Arrays.asList(removedHarvest, watering));
        var result = service.get(memberId, reportId);
        assertThat(result.getFeedbacks().stream().map(it -> it.getWorkType()).toList()).containsExactly(WorkType.WATERING);
    }

    @Test
    @DisplayName("active report is not exposed as report feedback")
    void active_report_is_not_exposed_as_report_feedback() throws Exception {
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(report(FarmingCycleReportStatus.ACTIVE, CycleReportStatistics.empty()));
        assertReportNotFound();
        verifyNoInteractions(feedbackRepository);
    }

    @Test
    @DisplayName("superseded report is not exposed as report feedback")
    void superseded_report_is_not_exposed_as_report_feedback() throws Exception {
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(report(FarmingCycleReportStatus.SUPERSEDED, CycleReportStatistics.empty()));
        assertReportNotFound();
        verifyNoInteractions(feedbackRepository);
    }

    @Test
    @DisplayName("report outside the member scope is not exposed")
    void report_outside_the_member_scope_is_not_exposed() throws Exception {
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(null);
        assertReportNotFound();
        verifyNoInteractions(feedbackRepository);
    }

    @Test
    @DisplayName("regenerate delegates stale feedback to the locked lifecycle")
    void regenerate_delegates_stale_feedback_to_the_locked_lifecycle() throws Exception {
        var stale = also(readyFeedback(), ReportFeedback::markStale);
        when(lifecycleService.regenerate(memberId, reportId, WorkType.WATERING)).thenAnswer(it -> {
        stale.retry("d".repeat(64));
        return stale;
        });
        var result = service.regenerate(memberId, reportId, WorkType.WATERING);
        assertThat(result.getWorkType()).isEqualTo(WorkType.WATERING);
        assertThat(result.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(result.getInputPrepared()).isFalse();
        assertThat(result.getFailureCode()).isNull();
        verify(reportRepository, never()).findByIdAndMember_Id(reportId, memberId);
    }

    @Test
    @DisplayName("regenerate propagates locked lifecycle validation without preloading the report")
    void regenerate_propagates_locked_lifecycle_validation_without_preloading_the_report() throws Exception {
        when(lifecycleService.regenerate(memberId, reportId, WorkType.HARVEST)).thenThrow(new BusinessException(ErrorCode.REPORT_FEEDBACK_NOT_FOUND));
        assertThatThrownBy(() -> service.regenerate(memberId, reportId, WorkType.HARVEST)).isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.REPORT_FEEDBACK_NOT_FOUND);
        verify(reportRepository, never()).findByIdAndMember_Id(reportId, memberId);
    }
    private void assertReportNotFound() {
        assertThatThrownBy(() -> service.get(memberId, reportId)).isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.REPORT_NOT_FOUND);
    }
    private ReportFeedback readyFeedback() {
        return also(feedback(wateringFeedbackId, WorkType.WATERING), it -> {
        it.attachInputSnapshot(map(new AbstractMap.SimpleEntry<>("schemaVersion", 2)));
        it.markReady("관수 작업 요약", java.util.Arrays.asList(new ReportFeedbackItemDraft(ReportFeedbackItemSection.COMPARISON, "직전보다 기록 1회 증가", "직전 재배보다 물 주기 기록이 한 번 늘었어요."), new ReportFeedbackItemDraft(ReportFeedbackItemSection.STRENGTH, "기록 근거", "수분 상태를 꾸준히 확인했어요."), new ReportFeedbackItemDraft(ReportFeedbackItemSection.IMPROVEMENT, "통계 근거", "관수 간격도 함께 비교해 보세요."), new ReportFeedbackItemDraft(ReportFeedbackItemSection.NEXT_ACTION, "기록 근거", "내일 토양 수분을 다시 확인하세요.")), java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("kind", "record"), new AbstractMap.SimpleEntry<>("id", "internal-only"))), "PASS", List.of(), "chat-model", "embedding-model");
        });
    }
    private ReportFeedback pendingFeedback() {
        return feedback(fertilizingFeedbackId, WorkType.FERTILIZING);
    }
    private ReportFeedback failedFeedback() {
        return also(feedback(harvestFeedbackId, WorkType.HARVEST), it -> {
        it.attachInputSnapshot(map(new AbstractMap.SimpleEntry<>("schemaVersion", 2)));
        it.markFailed("STRUCTURED_OUTPUT_INVALID");
        });
    }
    private ReportFeedback feedback(UUID id, WorkType workType) {
        return also(new ReportFeedback(id, member, completedReport, workType, ReportFeedbackStatus.PENDING, null, Integer.toString(workType.ordinal() + 1).repeat(64).substring(0, 64), List.of(), null, List.of(), null, null, null), it -> {
        ReflectionTestUtils.setField(it, "createdAt", createdAt);
        ReflectionTestUtils.setField(it, "updatedAt", updatedAt);
        });
    }
    private FarmingCycleReport report(FarmingCycleReportStatus status, CycleReportStatistics statistics) {
        var farm = new Farm(uuid("000000000301"), member, "약초농장", "강원도 평창군");
        var crop = new Crop(uuid("000000000401"), 422, "황기", CropUsePartCategory.ROOT_BARK);
        var finalHarvest = new FarmingRecord(uuid("000000000501"), member, farm, crop, WorkType.HARVEST, LocalDateTime.of(2026, 7, 1, 9, 0), "맑음", 24, "최종 수확", com.chamchamcham.domain.farming.EntryMode.MANUAL, false);
        var sourceStatus = status == FarmingCycleReportStatus.ACTIVE ? FarmingCycleReportStatus.ACTIVE : FarmingCycleReportStatus.COMPLETED;
        var report = FarmingCycleReport.create(member, farm, crop, new FarmingCycleReportProjection(sourceStatus, LocalDateTime.of(2026, 2, 1, 9, 0), sourceStatus == FarmingCycleReportStatus.COMPLETED ? finalHarvest.getWorkedAt() : null, FarmingCycleStartBasis.FIRST_RECORD, sourceStatus == FarmingCycleReportStatus.COMPLETED ? finalHarvest : null, 1, statistics));
        ReflectionTestUtils.setField(report, "id", reportId);
        if (status == FarmingCycleReportStatus.SUPERSEDED) report.supersede();
        return report;
    }
    private CycleReportStatistics statisticsFor(WorkType... workTypes) {
        return new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(countIn(WorkType.WATERING, workTypes), null, LocalDate.of(2026, 6, 2), 0, null, 0, null, List.of(), null, List.of(), List.of()), new FertilizingStatistics(countIn(WorkType.FERTILIZING, workTypes), null, LocalDate.of(2026, 6, 1), 0, null, 0, null, List.of(), null, null, null, new Coverage(0, 0), List.of(), List.of(), List.of(), List.of()), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), new HarvestStatistics(countIn(WorkType.HARVEST, workTypes), null, LocalDate.of(2026, 6, 3), 0, null, 0, null, List.of(), null, null, null, new Coverage(0, 0), null, null, List.of(), null, null, List.of()), CommonOnlyStatistics.empty());
    }
    private UUID uuid(String tail) {
        return UUID.fromString("00000000-0000-0000-0000-" + tail + "");
    }
    @SafeVarargs
    private static <K> Map<K, Object> map(Map.Entry<K, ?>... entries) {
        Map<K, Object> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private static <T> T also(T value, java.util.function.Consumer<T> action) { action.accept(value); return value; }

    private int countIn(WorkType type, WorkType[] types) { return Arrays.asList(types).contains(type) ? 1 : 0; }
}
