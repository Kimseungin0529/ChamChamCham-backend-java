package com.chamchamcham.domain.report;

import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmDataSource;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberStatus;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FarmingCycleReportTest {
    private final LocalDateTime workedAt = LocalDateTime.of(2026, 6, 15, 8, 30);
    private final Member member = new Member(UUID.randomUUID(), "member@example.com", null, MemberStatus.ACTIVE,
        null, null, null, null, null, null, "ROLE_USER", null, null);
    private final Farm farm = new Farm(UUID.randomUUID(), member, "약초농장", "강원도 평창군", null, null, null,
        null, null, null, false, new ArrayList<>(), new FarmDataSource(null, null, null, null));
    private final Crop crop = new Crop(UUID.randomUUID(), 422, "황기", CropUsePartCategory.ROOT_BARK);
    private final FarmingRecord finalHarvest = new FarmingRecord(UUID.randomUUID(), member, farm, crop, WorkType.HARVEST,
        workedAt, "맑음", 24, "최종 수확", EntryMode.MANUAL, false);

    @Test
    void sameProjectionKeepsRevisionAndChangedTypedStatisticsIncrementIt() {
        var initial = activeProjection(CycleReportStatistics.empty());
        var report = FarmingCycleReport.create(member, farm, crop, initial);
        var changed = activeProjection(withWateringCount(1));
        assertThat(report.getSourceRevision()).isEqualTo(1);
        assertThat(report.applyProjection(initial)).isFalse();
        assertThat(report.getSourceRevision()).isEqualTo(1);
        assertThat(report.applyProjection(changed)).isTrue();
        assertThat(report.getSourceRevision()).isEqualTo(2);
        assertThat(report.applyProjection(activeProjection(withWateringCount(1)))).isFalse();
    }

    @Test
    void activeRejectsAnEndBoundaryAndCompletedRequiresOne() {
        assertThatThrownBy(() -> activeReport(CycleReportStatistics.empty()).applyProjection(
            new FarmingCycleReportProjection(FarmingCycleReportStatus.ACTIVE, workedAt, workedAt,
                FarmingCycleStartBasis.FIRST_RECORD, finalHarvest, 1, CycleReportStatistics.empty())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> activeReport(CycleReportStatistics.empty()).applyProjection(
            new FarmingCycleReportProjection(FarmingCycleReportStatus.COMPLETED, workedAt.minusMonths(6), workedAt,
                FarmingCycleStartBasis.AFTER_PREVIOUS_FINAL_HARVEST, null, 1, CycleReportStatistics.empty())))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void supersedePreservesStatisticsAndIncrementsOnce() {
        var report = activeReport(withWateringCount(1));
        long revision = report.getSourceRevision();
        assertThat(report.supersede()).isTrue();
        assertThat(report.getStatistics().getWatering().getRecordCount()).isEqualTo(1);
        assertThat(report.getSourceRevision()).isEqualTo(revision + 1);
        assertThat(report.supersede()).isFalse();
    }

    @Test
    void rejectsInvalidSchemaVersionReverseBoundaryAndSupersededProjection() {
        for (var projection : new FarmingCycleReportProjection[] {
            new FarmingCycleReportProjection(FarmingCycleReportStatus.ACTIVE, workedAt, null, FarmingCycleStartBasis.FIRST_RECORD, null, 0, CycleReportStatistics.empty()),
            new FarmingCycleReportProjection(FarmingCycleReportStatus.COMPLETED, workedAt, workedAt.minusDays(1), FarmingCycleStartBasis.FIRST_RECORD, finalHarvest, 1, CycleReportStatistics.empty()),
            new FarmingCycleReportProjection(FarmingCycleReportStatus.SUPERSEDED, workedAt, null, FarmingCycleStartBasis.FIRST_RECORD, null, 1, CycleReportStatistics.empty())}) {
            assertThatThrownBy(() -> FarmingCycleReport.create(member, farm, crop, projection)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    private FarmingCycleReport activeReport(CycleReportStatistics statistics) {
        return FarmingCycleReport.create(member, farm, crop, activeProjection(statistics));
    }

    private FarmingCycleReportProjection activeProjection(CycleReportStatistics statistics) {
        return new FarmingCycleReportProjection(FarmingCycleReportStatus.ACTIVE, workedAt, null,
            FarmingCycleStartBasis.FIRST_RECORD, null, 1, statistics);
    }

    private CycleReportStatistics withWateringCount(int count) {
        return new CycleReportStatistics(PlantingStatistics.empty(),
            new WateringStatistics(count, null, null, 0, null, 0, null, java.util.List.of(), null, java.util.List.of(), java.util.List.of()),
            FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(),
            HarvestStatistics.empty(), CommonOnlyStatistics.empty());
    }
}
