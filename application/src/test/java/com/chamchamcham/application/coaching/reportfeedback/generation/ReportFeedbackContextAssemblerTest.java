package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.report.WeedingStatistics;
import com.chamchamcham.domain.report.PlantingStatistics;
import com.chamchamcham.domain.report.HarvestStatistics;
import com.chamchamcham.domain.report.PestControlStatistics;
import com.chamchamcham.domain.report.CommonOnlyStatistics;
import com.chamchamcham.application.coaching.reportfeedback.ReportFeedbackFailureCode;
import com.chamchamcham.application.coaching.reportfeedback.ReportFeedbackGenerationFailure;
import com.chamchamcham.application.report.CategoryRef;
import com.chamchamcham.application.report.CycleReportSourceRecord;
import com.chamchamcham.application.report.FarmingCyclePartitioner;
import com.chamchamcham.application.report.FarmingCycleReportSourceLoader;
import com.chamchamcham.application.report.HarvestReportSource;
import com.chamchamcham.application.report.ReportScope;
import com.chamchamcham.application.report.WateringReportSource;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.report.CycleReportStatistics;
import com.chamchamcham.domain.report.Coverage;
import com.chamchamcham.domain.report.FertilizingStatistics;
import com.chamchamcham.domain.report.FarmingCycleReport;
import com.chamchamcham.domain.report.FarmingCycleReportProjection;
import com.chamchamcham.domain.report.FarmingCycleReportQueryRepository;
import com.chamchamcham.domain.report.FarmingCycleReportRepository;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import com.chamchamcham.domain.report.WateringStatistics;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class ReportFeedbackContextAssemblerTest {
    private final UUID memberId = UUID.randomUUID();
    private final UUID farmId = UUID.randomUUID();
    private final UUID cropId = UUID.randomUUID();
    private final UUID reportId = UUID.randomUUID();
    private final UUID previousReportId = UUID.randomUUID();
    private final UUID previousFinalHarvestId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID targetWateringId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID targetFertilizingId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private final UUID targetFinalHarvestId = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private final LocalDateTime startsAt = LocalDateTime.of(2026, 3, 1, 9, 0);
    private final LocalDateTime endsAt = LocalDateTime.of(2026, 7, 1, 9, 0);
    @Mock private FarmingCycleReportRepository reportRepository;
    @Mock private FarmingCycleReportQueryRepository reportQueryRepository;
    @Mock private FarmingCycleReportSourceLoader sourceLoader;

    @Test
    @DisplayName("assemble selects the final harvest slice and keeps only the requested work type")
    void assemble_selects_the_final_harvest_slice_and_keeps_only_the_requested_work_type() throws Exception {
        var report = also(completedReport(reportId, targetFinalHarvestId, startsAt, endsAt, new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(4, null, null, 3, new BigDecimal("2.5"), 0, new BigDecimal("75"), List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty())), it -> setSourceRevision(it, 7));
        var previous = also(completedReport(previousReportId, UUID.randomUUID(), startsAt.minusYears(1), endsAt.minusYears(1), new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(3, null, null, 2, new BigDecimal("4"), 0, new BigDecimal("50"), List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty())), it -> setSourceRevision(it, 3));
        stubReports(report, previous);
        when(sourceLoader.load(new ReportScope(memberId, farmId, cropId))).thenReturn(cycleRecords());
        var context = assembler().assemble(memberId, reportId, WorkType.WATERING);
        assertThat(context.getSchemaVersion()).isEqualTo(3);
        assertThat(context.getWorkType()).isEqualTo(WorkType.WATERING);
        assertThat(context.getRecords().stream().map(ReportFeedbackRecord::getId).toList()).containsExactly(targetWateringId);
        assertThat(context.getRecords()).allMatch(it -> Objects.equals(it.getWorkType(), WorkType.WATERING));
        assertThat(context.getReport().getStatistics()).containsEntry("recordCount", 4).containsEntry("averageIntervalDays", new BigDecimal("2.5")).doesNotContainKeys("watering", "fertilizing", "harvest");
        assertThat(context.getReport().getSourceRevision()).isEqualTo(7);
        assertThat(context.getPreviousReport().getId()).isEqualTo(previousReportId);
        assertThat(context.getPreviousReport().getSourceRevision()).isEqualTo(3);
        assertThat(context.getPreviousReport().getStatistics()).containsEntry("recordCount", 3);
        assertThat(context.getComparisons().stream().map(ReportFeedbackComparison::getMetricKey).toList()).containsExactly("recordCount", "workedDayCount", "averageIntervalDays", "photoAttachmentRatePct");
        assertThat(context.getComparisons().getFirst().getDifference()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("assemble omits previous comparison when the prior report has no matching work records")
    void assemble_omits_previous_comparison_when_the_prior_report_has_no_matching_work_records() throws Exception {
        var report = completedReport(reportId, targetFinalHarvestId, startsAt, endsAt, new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(1, null, null, 0, null, 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty()));
        var previous = completedReport(previousReportId, UUID.randomUUID(), startsAt.minusYears(1), endsAt.minusYears(1), CycleReportStatistics.empty());
        stubReports(report, previous);
        when(sourceLoader.load(new ReportScope(memberId, farmId, cropId))).thenReturn(cycleRecords());
        var context = assembler().assemble(memberId, reportId, WorkType.WATERING);
        assertThat(context.getPreviousReport()).isNull();
        assertThat(context.getComparisons()).isEmpty();
        assertThat(context.getWarnings()).containsExactly("previous_work_type_unavailable");
    }

    @Test
    @DisplayName("assemble leaves comparisons empty when there is no previous report")
    void assemble_leaves_comparisons_empty_when_there_is_no_previous_report() throws Exception {
        var report = completedReport(reportId, targetFinalHarvestId, startsAt, endsAt, new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(1, null, null, 0, null, 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty()));
        stubReports(report, null);
        when(sourceLoader.load(new ReportScope(memberId, farmId, cropId))).thenReturn(cycleRecords());
        var context = assembler().assemble(memberId, reportId, WorkType.WATERING);
        assertThat(context.getPreviousReport()).isNull();
        assertThat(context.getComparisons()).isEmpty();
        assertThat(context.getWarnings()).containsExactly("previous_report_unavailable");
    }

    @Test
    @DisplayName("assemble calculates selected typed quantity before map conversion and preserves coverage")
    void assemble_calculates_selected_typed_quantity_before_map_conversion_and_preserves_coverage() throws Exception {
        var report = completedReport(reportId, targetFinalHarvestId, startsAt, endsAt, new CycleReportStatistics(PlantingStatistics.empty(), WateringStatistics.empty(), new FertilizingStatistics(2, null, null, 2, null, 0, null, List.of(), null, new BigDecimal("1.5000"), null, new Coverage(1, 2), List.of(), List.of(), List.of(), List.of()), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty()));
        var previous = completedReport(previousReportId, UUID.randomUUID(), startsAt.minusYears(1), endsAt.minusYears(1), new CycleReportStatistics(PlantingStatistics.empty(), WateringStatistics.empty(), new FertilizingStatistics(1, null, null, 1, null, 0, null, List.of(), null, new BigDecimal("1.0000"), null, new Coverage(1, 1), List.of(), List.of(), List.of(), List.of()), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty()));
        stubReports(report, previous);
        when(sourceLoader.load(new ReportScope(memberId, farmId, cropId))).thenReturn(cycleRecords());
        var comparison = assembler().assemble(memberId, reportId, WorkType.FERTILIZING).getComparisons().stream().filter(it -> Objects.equals(it.getMetricKey(), "fertilizing.totalAmountKg")).findFirst().orElseThrow();
        assertThat(comparison.getDifference()).isEqualByComparingTo("0.5");
        assertThat(comparison.getCurrentCoverage()).isEqualTo(new Coverage(1, 2));
        assertThat(comparison.getPreviousCoverage()).isEqualTo(new Coverage(1, 1));
    }

    @Test
    @DisplayName("assemble uses the canonical final harvest boundary when reports have the same end time")
    void assemble_uses_the_canonical_final_harvest_boundary_when_reports_have_the_same_end_time() throws Exception {
        var report = completedReport(reportId, targetFinalHarvestId, startsAt, endsAt, new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(1, null, null, 0, null, 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty()));
        var previous = completedReport(previousReportId, previousFinalHarvestId, startsAt.minusYears(1), endsAt, new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(2, null, null, 0, null, 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty()));
        stubReports(report, previous);
        when(sourceLoader.load(new ReportScope(memberId, farmId, cropId))).thenReturn(cycleRecords());
        var context = assembler().assemble(memberId, reportId, WorkType.WATERING);
        assertThat(context.getPreviousReport().getId()).isEqualTo(previousReportId);
        verify(reportQueryRepository).findPreviousCompleted(memberId, farmId, cropId, endsAt, targetFinalHarvestId);
    }

    @Test
    @DisplayName("assemble rejects a work type absent from the target slice")
    void assemble_rejects_a_work_type_absent_from_the_target_slice() throws Exception {
        var report = completedReport(reportId, targetFinalHarvestId, startsAt, endsAt, CycleReportStatistics.empty());
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(report);
        when(sourceLoader.load(new ReportScope(memberId, farmId, cropId))).thenReturn(cycleRecords());
        assertThatThrownBy(() -> assembler().assemble(memberId, reportId, WorkType.PRUNING)).isInstanceOfSatisfying(ReportFeedbackGenerationFailure.class, it -> assertThat(it.getCode()).isEqualTo(ReportFeedbackFailureCode.INVALID_CONTEXT));
    }
    private ReportFeedbackContextAssembler assembler() {
        return new ReportFeedbackContextAssembler(reportRepository, reportQueryRepository, sourceLoader, new FarmingCyclePartitioner(), com.fasterxml.jackson.module.kotlin.ExtensionsKt.jacksonObjectMapper(), new ReportFeedbackComparisonCalculator());
    }
    private void stubReports(FarmingCycleReport report, FarmingCycleReport previous) {
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(report);
        when(reportQueryRepository.findPreviousCompleted(memberId, farmId, cropId, Objects.requireNonNull(report.getEndsAt()), Objects.requireNonNull(report.getFinalHarvestRecord().getId()))).thenReturn(previous);
    }
    private List<CycleReportSourceRecord> cycleRecords() {
        return java.util.Arrays.asList(harvestSource(previousFinalHarvestId, startsAt, true), new CycleReportSourceRecord(targetWateringId, startsAt, startsAt, WorkType.WATERING, "맑음", 24, false, "점적관수로 수분을 보충했어요.", null, new WateringReportSource(new CategoryRef("LOW", "적음"), new CategoryRef("DRIP", "점적")), null, null, null, null), new CycleReportSourceRecord(targetFertilizingId, startsAt.plusDays(1), startsAt.plusDays(1), WorkType.FERTILIZING, "맑음", 25, false, "밑거름을 줬어요.", null, null, null, null, null, null), harvestSource(targetFinalHarvestId, endsAt, true));
    }
    private CycleReportSourceRecord harvestSource(UUID id, LocalDateTime workedAt, boolean isFinal) {
        return new CycleReportSourceRecord(id, workedAt, workedAt, WorkType.HARVEST, "맑음", 24, false, "마지막 수확이에요.", null, null, null, null, null, new HarvestReportSource(new BigDecimal("5.0"), new CategoryRef("ROOT_BARK", "뿌리/근피"), new CategoryRef("CULTIVATED", "재배"), null, null, 12, isFinal));
    }
    private FarmingCycleReport completedReport(UUID id, UUID finalHarvestId, LocalDateTime startsAt, LocalDateTime endsAt, CycleReportStatistics statistics) {
        var member = new Member(memberId, "member@example.com", null);
        var farm = new Farm(farmId, member, "약초농장", "강원도 평창군");
        var crop = new Crop(cropId, 422, "황기", CropUsePartCategory.ROOT_BARK);
        var harvest = new FarmingRecord(finalHarvestId, member, farm, crop, WorkType.HARVEST, endsAt, "맑음", 24, "최종 수확", com.chamchamcham.domain.farming.EntryMode.MANUAL, false);
        return also(FarmingCycleReport.create(member, farm, crop, new FarmingCycleReportProjection(FarmingCycleReportStatus.COMPLETED, startsAt, endsAt, FarmingCycleStartBasis.FIRST_RECORD, harvest, 1, statistics)), it -> setId(it, id));
    }
    private void setId(Object target, UUID id) { org.springframework.test.util.ReflectionTestUtils.setField(target, "id", id); }
    private void setSourceRevision(FarmingCycleReport target, long revision) { org.springframework.test.util.ReflectionTestUtils.setField(target, "sourceRevision", revision); }
    @SafeVarargs
    private static <K> Map<K, Object> map(Map.Entry<K, ?>... entries) {
        Map<K, Object> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private static <T> T also(T value, java.util.function.Consumer<T> action) { action.accept(value); return value; }

}
