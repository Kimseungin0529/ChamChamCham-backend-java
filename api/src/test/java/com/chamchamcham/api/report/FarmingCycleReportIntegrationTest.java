package com.chamchamcham.api.report;

import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.ApiApplication;
import com.chamchamcham.application.farming.FarmingRecordCommand;
import com.chamchamcham.application.farming.FarmingRecordService;
import com.chamchamcham.application.report.FarmingCycleReportQueryService;
import com.chamchamcham.application.report.FarmingCycleReportSearchCondition;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackRepository;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackRepository;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.crop.MemberCrop;
import com.chamchamcham.domain.crop.MemberCropRepository;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.farming.FarmingRecordMediaRepository;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.farming.HarvestRecordRepository;
import com.chamchamcham.domain.farming.HarvestSource;
import com.chamchamcham.domain.farming.IrrigationAmount;
import com.chamchamcham.domain.farming.IrrigationMethod;
import com.chamchamcham.domain.farming.WateringRecordRepository;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import com.chamchamcham.domain.report.CountDistribution;
import com.chamchamcham.domain.report.FarmingCycleReportRepository;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.GrowthPeriodRange;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@SpringBootTest(
    classes = {ApiApplication.class},
    properties = {
        "spring.datasource.url=jdbc:h2:mem:farming-cycle-report-integration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
    }
)
@ActiveProfiles("test")
class FarmingCycleReportIntegrationTest {
@Autowired private FarmingRecordService farmingRecordService;
@Autowired private FarmingCycleReportQueryService queryService;
@Autowired private RecordFeedbackRepository recordFeedbackRepository;
@Autowired private ReportFeedbackRepository reportFeedbackRepository;
@Autowired private FarmingCycleReportRepository reportRepository;
@Autowired private FarmingRecordRepository farmingRecordRepository;
@Autowired private WateringRecordRepository wateringRecordRepository;
@Autowired private HarvestRecordRepository harvestRecordRepository;
@Autowired private FarmingRecordMediaRepository farmingRecordMediaRepository;
@Autowired private MemberRepository memberRepository;
@Autowired private MemberCropRepository memberCropRepository;
@Autowired private FarmRepository farmRepository;
@Autowired private CropRepository cropRepository;
private Member member;
private Farm farm;
private Crop crop;
private UUID memberId() { return java.util.Objects.requireNonNull(member.getId()); }
private UUID farmId() { return java.util.Objects.requireNonNull(farm.getId()); }
private UUID cropId() { return java.util.Objects.requireNonNull(crop.getId()); }
@BeforeEach
void setUp() throws Exception {
reportFeedbackRepository.deleteAllInBatch();
reportRepository.deleteAllInBatch();
recordFeedbackRepository.deleteAllInBatch();
farmingRecordMediaRepository.deleteAllInBatch();
wateringRecordRepository.deleteAllInBatch();
harvestRecordRepository.deleteAllInBatch();
farmingRecordRepository.deleteAllInBatch();
memberCropRepository.deleteAllInBatch();
farmRepository.deleteAllInBatch();
cropRepository.deleteAllInBatch();
memberRepository.deleteAllInBatch();
member = memberRepository.save(new Member("report-member@example.com", null));
farm = farmRepository.save(new Farm(member, "통합검증농장", "강원도 평창군"));
crop = cropRepository.save(new Crop(7001, "황기", CropUsePartCategory.ROOT_BARK));
memberCropRepository.save(new MemberCrop(member, farm, crop));
}
@Test
@DisplayName("record CRUD rebuilds active and completed report end to end")
void record_CRUD_rebuilds_active_and_completed_report_end_to_end() throws Exception {
var wateringId = farmingRecordService.create(wateringCommand()).getId();
var active = reportRepository.findByMember_IdAndFarm_IdAndCrop_IdAndStatus(memberId(), farmId(), cropId(), FarmingCycleReportStatus.ACTIVE);
assertThat(active).isNotNull();
assertThat(active.getStatistics().getWatering().getRecordCount()).isEqualTo(1);
var finalHarvestId = farmingRecordService.create(harvestCommand(true, "30", false, CropUsePartCategory.ROOT_BARK, 4)).getId();
assertThat(reportRepository.findByMember_IdAndFarm_IdAndCrop_IdAndStatus(memberId(), farmId(), cropId(), FarmingCycleReportStatus.ACTIVE)).isNull();
var completed = loadOnlyCompletedReport();
assertThat(completed.getStatus()).isEqualTo(FarmingCycleReportStatus.COMPLETED);
assertThat(completed.getFinalHarvestRecordId()).isEqualTo(finalHarvestId);
assertThat(completed.getStatistics().getWatering().getRecordCount()).isEqualTo(1);
assertThat(completed.getStatistics().getHarvest().getTotalAmountKg())
.isEqualByComparingTo("30.0000");
farmingRecordService.delete(new FarmingRecordCommand.Delete(memberId(), wateringId));
var rebuilt = queryService.getDetail(memberId(), completed.getId());
assertThat(rebuilt.getSelected().getStatistics().getWatering().getRecordCount()).isZero();
assertThat(rebuilt.getSelected().getSourceRevision())
.isGreaterThan(completed.getSourceRevision());
}
@Test
@DisplayName("unknown optional harvest values still complete the cycle with required growth statistics")
void unknown_optional_harvest_values_still_complete_the_cycle_with_required_growth_statistics() throws Exception {
var finalHarvestId = farmingRecordService.create(harvestCommand(true, null, true, null, 4)).getId();
var completed = loadOnlyCompletedReport();
assertThat(completed.getStatus()).isEqualTo(FarmingCycleReportStatus.COMPLETED);
assertThat(completed.getFinalHarvestRecordId()).isEqualTo(finalHarvestId);
assertThat(completed.getStatisticsSchemaVersion()).isEqualTo(3);
assertThat(completed.getStatistics().getHarvest().getRecordCount()).isEqualTo(1);
assertThat(completed.getStatistics().getHarvest().getTotalAmountKg()).isNull();
assertThat(completed.getStatistics().getHarvest().getAmountCoverage().recordedCount()).isZero();
assertThat(completed.getStatistics().getHarvest().getAmountCoverage().targetCount()).isEqualTo(1);
assertThat(completed.getStatistics().getHarvest().getMedicinalParts()).isEmpty();
assertThat(completed.getStatistics().getHarvest().getFinalGrowthPeriodMonths()).isEqualTo(4);
assertThat(completed.getStatistics().getHarvest().getGrowthPeriodRangeMonths()).isEqualTo(new GrowthPeriodRange(4, 4));
assertThat(completed.getStatistics().getHarvest().getGrowthPeriodDistribution()).containsExactly(new CountDistribution("4", "4개월", 1, new BigDecimal("100.00")));
}
private com.chamchamcham.application.report.FarmingCycleReportResult.Snapshot loadOnlyCompletedReport() throws Exception {
        var items = queryService.listCompleted(new FarmingCycleReportSearchCondition(memberId(), Collections.singleton(farmId()), Collections.singleton(cropId()), null, 20)).getItems();
        assertThat(items).hasSize(1);
        return queryService.getDetail(memberId(), items.get(0).getId()).getSelected();
    }

private FarmingRecordCommand.Create wateringCommand() {
return new FarmingRecordCommand.Create(memberId(), farmId(), cropId(), WorkType.WATERING, LocalDateTime.of(2026, 3, 1, 9, 0), "맑음", 17, "관수", null, new FarmingRecordCommand.WateringDetail(IrrigationAmount.SUFFICIENT, IrrigationMethod.SPRAYING), null, null, null, null, java.util.Collections.emptyList(), EntryMode.MANUAL);
}
private FarmingRecordCommand.Create harvestCommand(boolean isLastHarvest, String amountKg, boolean amountUnknown, CropUsePartCategory medicinalPart, int growthPeriod) {
return new FarmingRecordCommand.Create(memberId(), farmId(), cropId(), WorkType.HARVEST, LocalDateTime.of(2026, 6, 30, 9, 0), "흐림", 25, "마지막 수확", null, null, null, null, null, new FarmingRecordCommand.HarvestDetail(amountKg == null ? null : new BigDecimal(amountKg), amountUnknown, medicinalPart, HarvestSource.CULTIVATED, growthPeriod, isLastHarvest), java.util.Collections.emptyList(), EntryMode.MANUAL);
}
}
