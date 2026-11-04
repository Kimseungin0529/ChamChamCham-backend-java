package com.chamchamcham.application.report;

import com.chamchamcham.application.coaching.reportfeedback.lifecycle.ReportFeedbackLifecycleService;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.report.FarmingCycleReport;
import com.chamchamcham.domain.report.FarmingCycleReportProjection;
import com.chamchamcham.domain.report.FarmingCycleReportRepository;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import com.chamchamcham.domain.farming.EntryMode;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(MockitoExtension.class)
class FarmingCycleReportProjectionServiceTest {
    private final UUID memberId = uuid("000000000001");
    private final UUID farmId = uuid("000000000101");
    private final UUID cropId = uuid("000000000201");
    private final UUID activeReportId = uuid("000000000301");
    private final UUID completedReportId = uuid("000000000302");
    private final UUID supersededReportId = uuid("000000000303");
    private final UUID finalHarvestRecordId = uuid("000000000401");
    private final UUID changedFinalHarvestRecordId = uuid("000000000402");
    private final UUID sameTimeFinalHarvestRecordId = uuid("000000000403");
    private final LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 9, 0);
    private final ReportScope scope = new ReportScope(memberId, farmId, cropId);
    @Mock private FarmRepository farmRepository;
    @Mock private CropRepository cropRepository;
    @Mock private FarmingRecordRepository farmingRecordRepository;
    @Mock private FarmingCycleReportSourceLoader sourceLoader;
    @Mock private FarmingCyclePartitioner partitioner;
    @Mock private FarmingCycleReportRepository reportRepository;
    @Mock private ReportFeedbackLifecycleService reportFeedbackLifecycleService;
    private FarmingCycleReportProjectionService service;
    private Member member;
    private Farm farm;
    private Crop crop;

    @BeforeEach
    void setUp() throws Exception {
        service = new FarmingCycleReportProjectionService(farmRepository, cropRepository, farmingRecordRepository, sourceLoader, partitioner,
            new CycleReportStatisticsCalculator(), reportRepository, reportFeedbackLifecycleService);
        member = new Member(memberId, "" + memberId + "@example.com", null);
        farm = new Farm(farmId, member, "약초농장", "서울시 강남구");
        crop = new Crop(cropId, 1001, "황기", CropUsePartCategory.ROOT_BARK);
    }

    @Test
    @DisplayName("rebuild locks owned farm before reading source records")
    void rebuildLocksOwnedFarmBeforeReadingSourceRecords() throws Exception {
        stubScope(List.of());
        var source = java.util.Arrays.asList(wateringRecord(1));
        when(sourceLoader.load(scope)).thenReturn(source);
        when(partitioner.partition(source)).thenReturn(java.util.Arrays.asList(activeSlice(1, FarmingCycleStartBasis.FIRST_RECORD,
            java.util.Arrays.asList(wateringRecord(1)))));
        service.rebuild(scope);
        var order = inOrder(farmRepository, sourceLoader);
        order.verify(farmRepository).findOwnedByIdForReportUpdate(farmId, memberId);
        order.verify(sourceLoader).load(scope);
    }

    @Test
    @DisplayName("first record creates active report")
    void firstRecordCreatesActiveReport() throws Exception {
        stubScope(List.of());
        var source = java.util.Arrays.asList(wateringRecord(1));
        when(sourceLoader.load(scope)).thenReturn(source);
        when(partitioner.partition(source)).thenReturn(java.util.Arrays.asList(activeSlice(1, FarmingCycleStartBasis.FIRST_RECORD,
            java.util.Arrays.asList(wateringRecord(1)))));
        service.rebuild(scope);
        var saved = single(capturedSavedReports());
        assertThat(saved.getStatus()).isEqualTo(FarmingCycleReportStatus.ACTIVE);
        assertThat(saved.getStatistics().getWatering().getRecordCount()).isEqualTo(1);
        assertThat(saved.getStatisticsSchemaVersion()).isEqualTo(3);
        assertThat(saved.getSourceRevision()).isEqualTo(1);
    }

    @Test
    @DisplayName("latest final harvest converts active report to completed")
    void latestFinalHarvestConvertsActiveReportToCompleted() throws Exception {
        var active = persistedActiveReport(activeReportId, baseTime.plusDays(1));
        stubScope(java.util.Arrays.asList(active));
        when(farmingRecordRepository.getReferenceById(finalHarvestRecordId)).thenReturn(referenceRecord(finalHarvestRecordId, 10));
        var source = recordsEndingWithFinalHarvest();
        when(sourceLoader.load(scope)).thenReturn(source);
        when(partitioner.partition(source)).thenReturn(java.util.Arrays.asList(completedSlice(finalHarvestRecordId, 10)));
        service.rebuild(scope);
        assertThat(active.getStatus()).isEqualTo(FarmingCycleReportStatus.COMPLETED);
        assertThat(active.getFinalHarvestRecord().getId()).isEqualTo(finalHarvestRecordId);
        assertThat(active.getEndsAt()).isEqualTo(baseTime.plusDays(10));
        verify(reportRepository).save(active);
        verify(reportFeedbackLifecycleService).reconcile(active, Set.of(WorkType.WATERING, WorkType.HARVEST));
    }

    @Test
    @DisplayName("active report does not enqueue report feedback")
    void activeReportDoesNotEnqueueReportFeedback() throws Exception {
        stubScope(List.of());
        var source = java.util.Arrays.asList(wateringRecord(1));
        when(sourceLoader.load(scope)).thenReturn(source);
        when(partitioner.partition(source)).thenReturn(java.util.Arrays.asList(activeSlice(1, FarmingCycleStartBasis.FIRST_RECORD,
            java.util.Arrays.asList(wateringRecord(1)))));
        service.rebuild(scope);
        verifyNoInteractions(reportFeedbackLifecycleService);
    }

    @Test
    @DisplayName("records after final preserve active report and create completed report")
    void recordsAfterFinalPreserveActiveReportAndCreateCompletedReport() throws Exception {
        var active = persistedActiveReport(activeReportId, baseTime.plusDays(1));
        stubScope(java.util.Arrays.asList(active));
        when(farmingRecordRepository.getReferenceById(finalHarvestRecordId)).thenReturn(referenceRecord(finalHarvestRecordId, 10));
        var source = new ArrayList<>(recordsEndingWithFinalHarvest());
        source.add(wateringRecord(20));
        when(sourceLoader.load(scope)).thenReturn(source);
        when(partitioner.partition(source)).thenReturn(java.util.Arrays.asList(completedSlice(finalHarvestRecordId, 10), activeSlice(20,
            FarmingCycleStartBasis.AFTER_PREVIOUS_FINAL_HARVEST, java.util.Arrays.asList(wateringRecord(20)))));
        service.rebuild(scope);
        var saved = capturedSavedReports();
        var createdCompleted = single(saved.stream().filter(it -> it!= active).toList());
        assertThat(active.getStatus()).isEqualTo(FarmingCycleReportStatus.ACTIVE);
        assertThat(active.getStartsAt()).isEqualTo(baseTime.plusDays(20));
        assertThat(createdCompleted.getStatus()).isEqualTo(FarmingCycleReportStatus.COMPLETED);
        assertThat(createdCompleted.getFinalHarvestRecord().getId()).isEqualTo(finalHarvestRecordId);
    }

    @Test
    @DisplayName("historical boundary matching uses final harvest id instead of timestamp")
    void historicalBoundaryMatchingUsesFinalHarvestIdInsteadOfTimestamp() throws Exception {
        var completed = persistedCompletedReport(completedReportId, finalHarvestRecordId, baseTime.plusDays(1), baseTime.plusDays(10), 1);
        var active = persistedActiveReport(activeReportId, baseTime.plusDays(20));
        stubScope(java.util.Arrays.asList(completed, active));
        when(farmingRecordRepository.getReferenceById(sameTimeFinalHarvestRecordId)).thenReturn(referenceRecord(sameTimeFinalHarvestRecordId, 10));
        var source = java.util.Arrays.asList(wateringRecord(1), finalHarvestRecord(sameTimeFinalHarvestRecordId, 10), wateringRecord(20));
        when(sourceLoader.load(scope)).thenReturn(source);
        when(partitioner.partition(source)).thenReturn(java.util.Arrays.asList(completedSlice(sameTimeFinalHarvestRecordId, 10), activeSlice(20,
            FarmingCycleStartBasis.AFTER_PREVIOUS_FINAL_HARVEST, java.util.Arrays.asList(wateringRecord(20)))));
        service.rebuild(scope);
        var createdCompleted = single(capturedSavedReports().stream().filter(it -> it!= active).toList());
        assertThat(completed.getStatus()).isEqualTo(FarmingCycleReportStatus.SUPERSEDED);
        assertThat(active.getStatus()).isEqualTo(FarmingCycleReportStatus.ACTIVE);
        assertThat(createdCompleted.getFinalHarvestRecord().getId()).isEqualTo(sameTimeFinalHarvestRecordId);
    }

    @Test
    @DisplayName("changed completed boundary creates new completed report and supersedes old one")
    void changedCompletedBoundaryCreatesNewCompletedReportAndSupersedesOldOne() throws Exception {
        var completed = persistedCompletedReport(completedReportId, finalHarvestRecordId, baseTime.plusDays(1), baseTime.plusDays(10), 1);
        stubScope(java.util.Arrays.asList(completed));
        when(farmingRecordRepository.getReferenceById(changedFinalHarvestRecordId)).thenReturn(referenceRecord(changedFinalHarvestRecordId, 11));
        var source = java.util.Arrays.asList(wateringRecord(1), finalHarvestRecord(changedFinalHarvestRecordId, 11));
        when(sourceLoader.load(scope)).thenReturn(source);
        when(partitioner.partition(source)).thenReturn(java.util.Arrays.asList(completedSlice(changedFinalHarvestRecordId, 11)));
        service.rebuild(scope);
        var createdCompleted = single(capturedSavedReports().stream().filter(it -> it!= completed).toList());
        assertThat(createdCompleted.getStatus()).isEqualTo(FarmingCycleReportStatus.COMPLETED);
        assertThat(createdCompleted.getFinalHarvestRecord().getId()).isEqualTo(changedFinalHarvestRecordId);
        assertThat(completed.getStatus()).isEqualTo(FarmingCycleReportStatus.SUPERSEDED);
    }

    @Test
    @DisplayName("removed final boundary merges into active and supersedes completed report")
    void removedFinalBoundaryMergesIntoActiveAndSupersedesCompletedReport() throws Exception {
        var completed = persistedCompletedReport(completedReportId, finalHarvestRecordId, baseTime.plusDays(1), baseTime.plusDays(10), 1);
        var active = persistedActiveReport(activeReportId, baseTime.plusDays(20));
        stubScope(java.util.Arrays.asList(completed, active));
        var source = java.util.Arrays.asList(wateringRecord(1), wateringRecord(20));
        when(sourceLoader.load(scope)).thenReturn(source);
        when(partitioner.partition(source)).thenReturn(java.util.Arrays.asList(activeSlice(1, FarmingCycleStartBasis.FIRST_RECORD, source)));
        service.rebuild(scope);
        assertThat(active.getStatus()).isEqualTo(FarmingCycleReportStatus.ACTIVE);
        assertThat(active.getStartsAt()).isEqualTo(baseTime.plusDays(1));
        assertThat(completed.getStatus()).isEqualTo(FarmingCycleReportStatus.SUPERSEDED);
    }

    @Test
    @DisplayName("empty source supersedes all current reports")
    void emptySourceSupersedesAllCurrentReports() throws Exception {
        var completed = persistedCompletedReport(completedReportId, finalHarvestRecordId, baseTime.plusDays(1), baseTime.plusDays(10), 1);
        var active = persistedActiveReport(activeReportId, baseTime.plusDays(1));
        stubScope(java.util.Arrays.asList(completed, active));
        when(sourceLoader.load(scope)).thenReturn(List.of());
        when(partitioner.partition(List.of())).thenReturn(List.of());
        service.rebuild(scope);
        assertThat(completed.getStatus()).isEqualTo(FarmingCycleReportStatus.SUPERSEDED);
        assertThat(active.getStatus()).isEqualTo(FarmingCycleReportStatus.SUPERSEDED);
        verify(reportRepository).saveAll(java.util.Arrays.asList(completed, active));
    }

    @Test
    @DisplayName("idempotent rebuild leaves source revision unchanged")
    void idempotentRebuildLeavesSourceRevisionUnchanged() throws Exception {
        var completed = persistedCompletedReport(completedReportId, finalHarvestRecordId, baseTime.plusDays(1), baseTime.plusDays(10), 3);
        var revision = completed.getSourceRevision();
        stubScope(java.util.Arrays.asList(completed));
        when(farmingRecordRepository.getReferenceById(finalHarvestRecordId)).thenReturn(referenceRecord(finalHarvestRecordId, 10));
        var source = recordsEndingWithFinalHarvest();
        when(sourceLoader.load(scope)).thenReturn(source);
        when(partitioner.partition(source)).thenReturn(java.util.Arrays.asList(completedSlice(finalHarvestRecordId, 10)));
        service.rebuild(scope);
        assertThat(completed.getSourceRevision()).isEqualTo(revision);
        verify(reportRepository).save(completed);
        verify(reportFeedbackLifecycleService).reconcile(completed, Set.of(WorkType.WATERING, WorkType.HARVEST));
    }

    @Test
    @DisplayName("rebuildAll deduplicates and sorts scopes before rebuilding")
    void rebuildAllDeduplicatesAndSortsScopesBeforeRebuilding() throws Exception {
        var scopeA = new ReportScope(uuid("000000000010"), uuid("000000000010"), uuid("000000000010"));
        var scopeB = new ReportScope(uuid("000000000010"), uuid("000000000011"), uuid("000000000010"));
        var scopeC = new ReportScope(uuid("000000000011"), uuid("000000000010"), uuid("000000000010"));
        java.util.Arrays.asList(scopeA, scopeB, scopeC).forEach(sortedScope -> {
            when(farmRepository.findOwnedByIdForReportUpdate(sortedScope.getFarmId(), sortedScope.getMemberId())).thenReturn(farm);
            when(cropRepository.findById(sortedScope.getCropId())).thenReturn(Optional.of(crop));
            when(reportRepository.findAllCurrent(sortedScope.getMemberId(), sortedScope.getFarmId(), sortedScope.getCropId())).thenReturn(List.of());
            when(sourceLoader.load(sortedScope)).thenReturn(List.of());
        });
        when(partitioner.partition(List.of())).thenReturn(List.of());
        service.rebuildAll(java.util.Arrays.asList(scopeC, scopeB, scopeA, scopeB));
        var order = inOrder(farmRepository);
        order.verify(farmRepository).findOwnedByIdForReportUpdate(scopeA.getFarmId(), scopeA.getMemberId());
        order.verify(farmRepository).findOwnedByIdForReportUpdate(scopeB.getFarmId(), scopeB.getMemberId());
        order.verify(farmRepository).findOwnedByIdForReportUpdate(scopeC.getFarmId(), scopeC.getMemberId());
    }
    private void stubScope(List<FarmingCycleReport> existing) {
        when(farmRepository.findOwnedByIdForReportUpdate(farmId, memberId)).thenReturn(farm);
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        when(reportRepository.findAllCurrent(memberId, farmId, cropId)).thenReturn(existing);
    }
    private CycleSlice activeSlice(long day, FarmingCycleStartBasis startBasis, List<CycleReportSourceRecord> records) {
        return new CycleSlice(FarmingCycleReportStatus.ACTIVE, startBasis, records);
    }
    private CycleSlice completedSlice(UUID finalRecordId, long day) {
        return new CycleSlice(FarmingCycleReportStatus.COMPLETED, FarmingCycleStartBasis.FIRST_RECORD, java.util.Arrays.asList(wateringRecord(1),
            finalHarvestRecord(finalRecordId, day)));
    }
    private List<CycleReportSourceRecord> recordsEndingWithFinalHarvest() {
        return java.util.Arrays.asList(wateringRecord(1), finalHarvestRecord(finalHarvestRecordId, 10));
    }
    private CycleReportSourceRecord wateringRecord(long day) {
        return new CycleReportSourceRecord(uuid("0000000005%02d".formatted(day)), baseTime.plusDays(day), (baseTime.plusDays(day)), WorkType.WATERING,
            "맑음", 20, false, "", null, new WateringReportSource(new CategoryRef("LOW", "적음"), new CategoryRef("HOSE", "호스")), null, null, null, null);
    }
    private CycleReportSourceRecord finalHarvestRecord(UUID id, long day) {
        return new CycleReportSourceRecord(id, baseTime.plusDays(day), (baseTime.plusDays(day)), WorkType.HARVEST, "맑음", 20, false, "", null, null, null,
            null, null, new HarvestReportSource(null, new CategoryRef("ROOT_BARK", "뿌리·껍질"), new CategoryRef("CULTIVATED", "재배"), null, null, 12, true));
    }
    private FarmingCycleReport persistedActiveReport(UUID id, LocalDateTime startsAt) {
        return persistedReport(id, new FarmingCycleReportProjection(FarmingCycleReportStatus.ACTIVE, startsAt, null, FarmingCycleStartBasis.FIRST_RECORD,
            null, 1, new CycleReportStatisticsCalculator().calculate(java.util.Arrays.asList(wateringRecord(1)))));
    }
    private FarmingCycleReport persistedCompletedReport(UUID id, UUID finalRecordId, LocalDateTime startsAt, LocalDateTime endsAt, int statisticsSchemaVersion) {
        return persistedReport(id, new FarmingCycleReportProjection(FarmingCycleReportStatus.COMPLETED, startsAt, endsAt,
            FarmingCycleStartBasis.FIRST_RECORD, referenceRecord(finalRecordId, 10), statisticsSchemaVersion,
            new CycleReportStatisticsCalculator().calculate(java.util.Arrays.asList(wateringRecord(1), finalHarvestRecord(finalRecordId, 10)))));
    }
    private FarmingCycleReport persistedReport(UUID id, FarmingCycleReportProjection projection) {
        var report = FarmingCycleReport.create(member, farm, crop, projection);
        org.springframework.test.util.ReflectionTestUtils.setField(report, "id", id);
        return report;
    }
    private FarmingRecord referenceRecord(UUID id, long day) {
        return new FarmingRecord(id, member, farm, crop, WorkType.HARVEST, baseTime.plusDays(day), "맑음", 20, "memo",
            com.chamchamcham.domain.farming.EntryMode.MANUAL, false);
    }
    private List<FarmingCycleReport> capturedSavedReports() {
        var captor = ArgumentCaptor.forClass(FarmingCycleReport.class);
        verify(reportRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }
    private UUID uuid(String tail) {
        return UUID.fromString("00000000-0000-0000-0000-" + tail + "");
    }
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void reconciliationReceivesDistinctWorkTypesInRecordEncounterOrder() {
        stubScope(List.of());
        var source = new ArrayList<CycleReportSourceRecord>();
        var types = List.of(WorkType.ETC, WorkType.PLANTING, WorkType.PEST_CONTROL, WorkType.WATERING,
            WorkType.FERTILIZING, WorkType.WEEDING, WorkType.PRUNING);
        for (int index = 0; index < types.size(); index++) {
            var fixture = wateringRecord(index + 1);
            source.add(new CycleReportSourceRecord(fixture.getId(), fixture.getWorkedAt(), fixture.getCreatedAt(), types.get(index),
                "맑음", 20, false, "", null, null, null, null, null, null));
        }
        source.add(finalHarvestRecord(finalHarvestRecordId, 10));
        when(sourceLoader.load(scope)).thenReturn(source);
        when(partitioner.partition(source)).thenReturn(List.of(new CycleSlice(FarmingCycleReportStatus.COMPLETED, FarmingCycleStartBasis.FIRST_RECORD, source)));
        when(farmingRecordRepository.getReferenceById(finalHarvestRecordId)).thenReturn(referenceRecord(finalHarvestRecordId, 10));
        service.rebuild(scope);
        ArgumentCaptor<Set<WorkType>> captor = (ArgumentCaptor) ArgumentCaptor.forClass(Set.class);
        verify(reportFeedbackLifecycleService).reconcile(org.mockito.ArgumentMatchers.any(FarmingCycleReport.class), captor.capture());
        assertThat(captor.getValue()).containsExactly(WorkType.ETC, WorkType.PLANTING, WorkType.PEST_CONTROL, WorkType.WATERING,
            WorkType.FERTILIZING, WorkType.WEEDING, WorkType.PRUNING, WorkType.HARVEST);
    }

    private static <T> T single(List<T> values) {
        assertThat(values).hasSize(1);
        return values.getFirst();
    }


    @Test
    void reconciliationSingletonWorkTypesRejectStructuralMutation() {
        var workTypes = capturedReconciliationTypes(List.of(finalHarvestRecord(finalHarvestRecordId, 10)));
        assertThat(workTypes).containsExactly(WorkType.HARVEST);
        assertThat(workTypes.contains(null)).isFalse();
        assertThat(workTypes.remove(null)).isFalse();
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> workTypes.add(null));
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> workTypes.add(WorkType.WATERING));
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> workTypes.remove(WorkType.HARVEST));
    }

    @Test
    void reconciliationMultiWorkTypesRetainEncounterOrderAndNullMutation() {
        var workTypes = capturedReconciliationTypes(List.of(wateringRecord(1), finalHarvestRecord(finalHarvestRecordId, 10)));
        assertThat(workTypes).containsExactly(WorkType.WATERING, WorkType.HARVEST);
        assertThat(workTypes.add(null)).isTrue();
        assertThat(workTypes).containsExactly(WorkType.WATERING, WorkType.HARVEST, null);
        assertThat(workTypes.remove(null)).isTrue();
        assertThat(workTypes.remove(WorkType.WATERING)).isTrue();
        assertThat(workTypes.add(WorkType.WATERING)).isTrue();
        assertThat(workTypes.add(WorkType.HARVEST)).isFalse();
        assertThat(workTypes).containsExactly(WorkType.HARVEST, WorkType.WATERING);
    }

    @Test
    void reconciliationRepeatedTypeAcrossMultipleRecordsRetainsMutableSet() {
        var time = baseTime.plusDays(1);
        var partial = new CycleReportSourceRecord(uuid("000000000402"), time, time, WorkType.HARVEST, "맑음", 20, false, "",
            null, null, null, null, null,
            new HarvestReportSource(null, new CategoryRef("ROOT_BARK", "뿌리·껍질"), new CategoryRef("CULTIVATED", "재배"), null, null, 12, false));
        var workTypes = capturedReconciliationTypes(List.of(partial, finalHarvestRecord(finalHarvestRecordId, 10)));
        assertThat(workTypes).containsExactly(WorkType.HARVEST);
        assertThat(workTypes.add(WorkType.WATERING)).isTrue();
        assertThat(workTypes.add(null)).isTrue();
        assertThat(workTypes).containsExactly(WorkType.HARVEST, WorkType.WATERING, null);
        assertThat(workTypes.remove(null)).isTrue();
        assertThat(workTypes.remove(WorkType.HARVEST)).isTrue();
        assertThat(workTypes).containsExactly(WorkType.WATERING);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Set<WorkType> capturedReconciliationTypes(List<CycleReportSourceRecord> source) {
        stubScope(List.of());
        when(sourceLoader.load(scope)).thenReturn(source);
        when(partitioner.partition(source)).thenReturn(List.of(new CycleSlice(FarmingCycleReportStatus.COMPLETED, FarmingCycleStartBasis.FIRST_RECORD, source)));
        when(farmingRecordRepository.getReferenceById(finalHarvestRecordId)).thenReturn(referenceRecord(finalHarvestRecordId, 10));
        service.rebuild(scope);
        ArgumentCaptor<Set<WorkType>> captor = (ArgumentCaptor) ArgumentCaptor.forClass(Set.class);
        verify(reportFeedbackLifecycleService).reconcile(org.mockito.ArgumentMatchers.any(FarmingCycleReport.class), captor.capture());
        return captor.getValue();
    }

}
