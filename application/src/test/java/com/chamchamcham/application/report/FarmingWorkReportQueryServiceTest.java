package com.chamchamcham.application.report;

import com.chamchamcham.domain.report.WeedingStatistics;
import com.chamchamcham.domain.report.PlantingStatistics;
import com.chamchamcham.domain.report.FertilizingStatistics;
import com.chamchamcham.domain.report.HarvestStatistics;
import com.chamchamcham.domain.report.PestControlStatistics;
import com.chamchamcham.domain.report.CommonOnlyStatistics;
import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.coaching.reportfeedback.lifecycle.ReportFeedbackDetailResult;
import com.chamchamcham.application.coaching.reportfeedback.lifecycle.ReportFeedbackItemResult;
import com.chamchamcham.application.coaching.reportfeedback.lifecycle.ReportFeedbackListResult;
import com.chamchamcham.application.coaching.reportfeedback.lifecycle.ReportFeedbackQueryService;
import com.chamchamcham.application.coaching.reportfeedback.lifecycle.ReportFeedbackResultContent;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackStatus;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.FarmingWorkReportSourceRepository;
import com.chamchamcham.domain.farming.FarmingWorkReportSourceSnapshot;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.report.FarmingCycleReportQueryRepository;
import com.chamchamcham.domain.report.CycleReportStatistics;
import com.chamchamcham.domain.report.FarmingCycleReport;
import com.chamchamcham.domain.report.FarmingCycleReportProjection;
import com.chamchamcham.domain.report.FarmingCycleReportRepository;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import com.chamchamcham.domain.report.WateringStatistics;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(MockitoExtension.class)
class FarmingWorkReportQueryServiceTest {
    private final UUID memberId = id("001");
    private final UUID farmId = id("101");
    private final UUID cropId = id("201");
    private final UUID reportId = id("301");
    private final UUID finalHarvestId = id("406");
    private final LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 9, 0);
    private final OpaqueCursorCodec cursorCodec = new OpaqueCursorCodec();
    private final Member member = new Member(memberId, "member@example.com", null);
    private final Farm farm = new Farm(farmId, member, "약초농장", "서울시 강남구");
    private final Crop crop = new Crop(cropId, 422, "황기", CropUsePartCategory.ROOT_BARK);
    @Mock private FarmingCycleReportQueryRepository queryRepository;
    @Mock private FarmingWorkReportSourceRepository sourceRepository;
    @Mock private FarmingCycleReportRepository reportRepository;
    @Mock private ReportFeedbackQueryService feedbackQueryService;
    private FarmingWorkReportQueryService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new FarmingWorkReportQueryService(queryRepository, sourceRepository, new FarmingCyclePartitioner(), cursorCodec, reportRepository,
            feedbackQueryService);
    }

    @Test
    @DisplayName("detail returns one typed statistics branch and ready feedback for an owned completed work")
    void detailReturnsOneTypedStatisticsBranchAndReadyFeedbackForAnOwnedCompletedWork() throws Exception {
        var report = report(FarmingCycleReportStatus.COMPLETED, new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(3,
            LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 4), 3, null, 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(),
            PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(),
            CommonOnlyStatistics.empty()));
        var feedbackContent = new ReportFeedbackResultContent("물 주기 작업을 안정적으로 이어갔어요.",
            java.util.Arrays.asList(new ReportFeedbackItemResult("흙 상태를 꾸준히 살폈어요.")), List.of(),
            java.util.Arrays.asList(new ReportFeedbackItemResult("내일 흙을 다시 확인하세요.")),
            java.util.Arrays.asList(new ReportFeedbackItemResult("직전 재배보다 물 주기 기록이 한 번 늘었어요.")));
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(report);
        when(feedbackQueryService.get(memberId, reportId)).thenReturn(feedbacks(feedback(WorkType.WATERING, ReportFeedbackStatus.READY, feedbackContent)));
        var detail = service.getDetail(memberId, reportId, WorkType.WATERING);
        assertThat(detail.getReportId()).isEqualTo(reportId);
        assertThat(detail.getStatus()).isEqualTo(FarmingCycleReportStatus.COMPLETED);
        assertThat(detail.getWorkType()).isEqualTo(WorkType.WATERING);
        assertThat(detail.getWorkTypeLabel()).isEqualTo("물 주기");
        assertThat(detail.getFarmId()).isEqualTo(farmId);
        assertThat(detail.getFarmName()).isEqualTo("약초농장");
        assertThat(detail.getCropId()).isEqualTo(cropId);
        assertThat(detail.getCropName()).isEqualTo("황기");
        assertThat(detail.getStartsAt()).isEqualTo(day(1));
        assertThat(detail.getEndsAt()).isEqualTo(day(5));
        assertThat(detail.getStatistics().getCommon().getRecordCount()).isEqualTo(3);
        assertThat(detail.getStatistics().getWatering().getRecordCount()).isEqualTo(3);
        assertThat(detail.getStatistics().getPlanting()).isNull();
        assertThat(detail.getStatistics().getFertilizing()).isNull();
        assertThat(detail.getStatistics().getPestControl()).isNull();
        assertThat(detail.getStatistics().getWeeding()).isNull();
        assertThat(detail.getStatistics().getHarvest()).isNull();
        var detailFeedback = Objects.requireNonNull(detail.getFeedback());
        assertThat(detailFeedback.getStatus()).isEqualTo(ReportFeedbackStatus.READY);
        assertThat(detailFeedback.getContent()).isSameAs(feedbackContent);
        assertThat(detailFeedback.getContent().getComparisons().stream().map(it -> it.getText()).toList()).containsExactly("직전 재배보다 물 주기 기록이 한 번 늘었어요.");
        verify(feedbackQueryService).get(memberId, reportId);
    }

    @Test
    @DisplayName("detail hides a report outside the member scope")
    void detailHidesAReportOutsideTheMemberScope() throws Exception {
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(null);
        assertDetailError(WorkType.WATERING, ErrorCode.REPORT_NOT_FOUND);
        verifyNoInteractions(feedbackQueryService);
    }

    @Test
    @DisplayName("detail returns active statistics without requesting coaching")
    void detailReturnsActiveStatisticsWithoutRequestingCoaching() throws Exception {
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(report(FarmingCycleReportStatus.ACTIVE,
            new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(2, null, null, 2, null, 0, null, List.of(), null, List.of(),
            List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(),
            HarvestStatistics.empty(), CommonOnlyStatistics.empty())));
        var detail = service.getDetail(memberId, reportId, WorkType.WATERING);
        assertThat(detail.getStatus()).isEqualTo(FarmingCycleReportStatus.ACTIVE);
        assertThat(detail.getEndsAt()).isNull();
        assertThat(detail.getStatistics().getCommon().getRecordCount()).isEqualTo(2);
        assertThat(detail.getStatistics().getWatering().getWorkedDayCount()).isEqualTo(2);
        assertThat(detail.getFeedback()).isNull();
        verifyNoInteractions(feedbackQueryService);
    }

    @Test
    @DisplayName("detail hides superseded reports")
    void detailHidesSupersededReports() throws Exception {
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(report(FarmingCycleReportStatus.SUPERSEDED,
            new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(1, null, null, 0, null, 0, null, List.of(), null, List.of(),
            List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(),
            HarvestStatistics.empty(), CommonOnlyStatistics.empty())));
        assertDetailError(WorkType.WATERING, ErrorCode.REPORT_NOT_FOUND);
        verifyNoInteractions(feedbackQueryService);
    }

    @Test
    @DisplayName("detail reports an absent work only after owned completed report validation")
    void detailReportsAnAbsentWorkOnlyAfterOwnedCompletedReportValidation() throws Exception {
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(report(FarmingCycleReportStatus.COMPLETED, CycleReportStatistics.empty()));
        assertDetailError(WorkType.WATERING, ErrorCode.WORK_REPORT_NOT_FOUND);
        verifyNoInteractions(feedbackQueryService);
    }

    @Test
    @DisplayName("detail exposes common only statistics for pruning")
    void detailExposesCommonOnlyStatisticsForPruning() throws Exception {
        var report = report(FarmingCycleReportStatus.COMPLETED, new CycleReportStatistics(PlantingStatistics.empty(), WateringStatistics.empty(),
            FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), new CommonOnlyStatistics(2, null, null, 2, null, 0,
            null, List.of(), null), HarvestStatistics.empty(), CommonOnlyStatistics.empty()));
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(report);
        when(feedbackQueryService.get(memberId, reportId)).thenReturn(feedbacks());
        var detail = service.getDetail(memberId, reportId, WorkType.PRUNING);
        assertThat(detail.getWorkTypeLabel()).isEqualTo("가지 정리");
        assertThat(detail.getStatistics().getCommon().getRecordCount()).isEqualTo(2);
        assertThat(detail.getStatistics().getPlanting()).isNull();
        assertThat(detail.getStatistics().getWatering()).isNull();
        assertThat(detail.getStatistics().getFertilizing()).isNull();
        assertThat(detail.getStatistics().getPestControl()).isNull();
        assertThat(detail.getStatistics().getWeeding()).isNull();
        assertThat(detail.getStatistics().getHarvest()).isNull();
    }

    @Test
    @DisplayName("detail keeps pending failed and transiently absent feedback content null")
    void detailKeepsPendingFailedAndTransientlyAbsentFeedbackContentNull() throws Exception {
        var report = report(FarmingCycleReportStatus.COMPLETED, new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(1, null, null,
            0, null, 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(),
            WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty()));
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(report);
        java.util.Arrays.asList(ReportFeedbackStatus.PENDING, ReportFeedbackStatus.FAILED).forEach(status -> {
            when(feedbackQueryService.get(memberId, reportId)).thenReturn(feedbacks(feedback(WorkType.WATERING, status, null)));
            var detail = service.getDetail(memberId, reportId, WorkType.WATERING);
            var detailFeedback = Objects.requireNonNull(detail.getFeedback());
            assertThat(detailFeedback.getStatus()).isEqualTo(status);
            assertThat(detailFeedback.getContent()).isNull();
        });
        when(feedbackQueryService.get(memberId, reportId)).thenReturn(feedbacks());
        var transientlyAbsent = service.getDetail(memberId, reportId, WorkType.WATERING);
        var transientFeedback = Objects.requireNonNull(transientlyAbsent.getFeedback());
        assertThat(transientFeedback.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(transientFeedback.getContent()).isNull();
    }

    @Test
    @DisplayName("list uses lookahead and exact cycle thumbnails with latest pictured fallback")
    void listUsesLookaheadAndExactCycleThumbnailsWithLatestPicturedFallback() throws Exception {
        var expectedCondition = new FarmingCycleReportQueryRepository.WorkItemSearchCondition(memberId, Set.of(farmId), Set.of(cropId), List.of(), null, 3);
        var watering = workItem(WorkType.WATERING, 2, this.reportId, this.farm, this.crop, FarmingCycleReportStatus.COMPLETED, day(5), finalHarvestId);
        var harvest = workItem(WorkType.HARVEST, 1, this.reportId, this.farm, this.crop, FarmingCycleReportStatus.COMPLETED, day(5), finalHarvestId);
        var lookahead = workItem(WorkType.ETC, 1, this.reportId, this.farm, this.crop, FarmingCycleReportStatus.COMPLETED, day(5), finalHarvestId);
        when(queryRepository.searchWorkItems(expectedCondition)).thenReturn(new FarmingCycleReportQueryRepository.WorkItemSearchResult(java.util.Arrays.asList(watering,
            harvest, lookahead)));
        var previousWatering = record("401", WorkType.WATERING, 1, 0, this.farm, this.crop);
        var previousFinal = record("402", WorkType.HARVEST, 2, 0, this.farm, this.crop);
        var picturedWatering = record("403", WorkType.WATERING, 3, 1, this.farm, this.crop);
        var latestWateringWithoutPhoto = record("404", WorkType.WATERING, 3, 2, this.farm, this.crop);
        var otherWorkType = record("405", WorkType.PRUNING, 4, 0, this.farm, this.crop);
        var targetFinal = record("406", WorkType.HARVEST, 5, 0, this.farm, this.crop);
        var adjacentWatering = record("407", WorkType.WATERING, 6, 0, this.farm, this.crop);
        var adjacentFinal = record("408", WorkType.HARVEST, 7, 0, this.farm, this.crop);
        when(sourceRepository.load(memberId, Set.of(farmId),
            Set.of(cropId))).thenReturn(new FarmingWorkReportSourceSnapshot(java.util.Arrays.asList(adjacentFinal, latestWateringWithoutPhoto, targetFinal,
            previousFinal, adjacentWatering, picturedWatering, previousWatering, otherWorkType), Set.of(Objects.requireNonNull(previousFinal.getId()),
            Objects.requireNonNull(targetFinal.getId()), Objects.requireNonNull(adjacentFinal.getId())),
            Map.ofEntries(Map.entry(Objects.requireNonNull(previousWatering.getId()), "https://img/previous-cycle.jpg"),
            Map.entry(Objects.requireNonNull(picturedWatering.getId()), "https://img/watering.jpg"),
            Map.entry(Objects.requireNonNull(otherWorkType.getId()), "https://img/other-work-type.jpg"),
            Map.entry(Objects.requireNonNull(targetFinal.getId()), "https://img/harvest.jpg"), Map.entry(Objects.requireNonNull(adjacentWatering.getId()),
            "https://img/adjacent-cycle.jpg"))));
        var page = service.list(new FarmingWorkReportSearchCondition(memberId, Set.of(farmId), Set.of(cropId), List.of(), null, 2));
        assertThat(page.getItems().stream().map(it -> it.getWorkType()).toList()).containsExactly(WorkType.WATERING, WorkType.HARVEST);
        assertThat(page.getItems()).allMatch(it -> Objects.equals(it.getStatus(), FarmingCycleReportStatus.COMPLETED));
        assertThat(page.getItems().getFirst().getThumbnailUrl()).isEqualTo("https://img/watering.jpg");
        assertThat(page.getItems().getLast().getThumbnailUrl()).isEqualTo("https://img/harvest.jpg");
        var decoded = cursorCodec.decode(Objects.requireNonNull(page.getNextCursor()), FarmingWorkReportCursorPayload.class);
        assertThat(decoded.getVersion()).isEqualTo(FarmingWorkReportCursorPayload.CURRENT_VERSION);
        assertThat(decoded.getLastWorkedOn()).isEqualTo(page.getItems().getLast().getLastWorkedOn());
        assertThat(decoded.getStatus()).isEqualTo(FarmingCycleReportStatus.COMPLETED);
        assertThat(decoded.getSortAt()).isEqualTo(page.getItems().getLast().getEndsAt());
        assertThat(decoded.getReportId()).isEqualTo(page.getItems().getLast().getReportId());
        assertThat(decoded.getWorkType()).isEqualTo(page.getItems().getLast().getWorkType());
        verify(queryRepository).searchWorkItems(expectedCondition);
        verify(sourceRepository).load(memberId, Set.of(farmId), Set.of(cropId));
    }

    @Test
    @DisplayName("list resolves active cycle thumbnail without a final harvest")
    void listResolvesActiveCycleThumbnailWithoutAFinalHarvest() throws Exception {
        var expectedCondition = new FarmingCycleReportQueryRepository.WorkItemSearchCondition(memberId, Set.of(farmId), Set.of(cropId),
            java.util.Arrays.asList(WorkType.WATERING), null, 2);
        var activeItem = workItem(WorkType.WATERING, 2, this.reportId, this.farm, this.crop, FarmingCycleReportStatus.ACTIVE, null, null);
        when(queryRepository.searchWorkItems(expectedCondition)).thenReturn(new FarmingCycleReportQueryRepository.WorkItemSearchResult(java.util.Arrays.asList(activeItem)));
        var picturedWatering = record("403", WorkType.WATERING, 3, 0, this.farm, this.crop);
        when(sourceRepository.load(memberId, Set.of(farmId),
            Set.of(cropId))).thenReturn(new FarmingWorkReportSourceSnapshot(java.util.Arrays.asList(picturedWatering), Set.of(),
            Map.ofEntries(Map.entry(Objects.requireNonNull(picturedWatering.getId()), "https://img/active-watering.jpg"))));
        var page = service.list(new FarmingWorkReportSearchCondition(memberId, Set.of(farmId), Set.of(cropId), java.util.Arrays.asList(WorkType.WATERING), null, 1));
        assertThat(single(page.getItems()).getStatus()).isEqualTo(FarmingCycleReportStatus.ACTIVE);
        assertThat(single(page.getItems()).getEndsAt()).isNull();
        assertThat(single(page.getItems()).getThumbnailUrl()).isEqualTo("https://img/active-watering.jpg");
        assertThat(page.getNextCursor()).isNull();
        verify(queryRepository).searchWorkItems(expectedCondition);
    }

    @Test
    @DisplayName("list decodes item cursor and returns null thumbnail without a pictured matching record")
    void listDecodesItemCursorAndReturnsNullThumbnailWithoutAPicturedMatchingRecord() throws Exception {
        var cursorPayload = new FarmingWorkReportCursorPayload(FarmingWorkReportCursorPayload.CURRENT_VERSION, day(29).toLocalDate(),
            FarmingCycleReportStatus.ACTIVE, day(30), id("399"), WorkType.WATERING);
        var cursor = cursorCodec.encode(cursorPayload);
        var expectedCondition = new FarmingCycleReportQueryRepository.WorkItemSearchCondition(memberId, Set.of(), Set.of(),
            java.util.Arrays.asList(WorkType.PEST_CONTROL), new FarmingCycleReportQueryRepository.WorkItemCursor(cursorPayload.getLastWorkedOn(),
            cursorPayload.getStatus(), cursorPayload.getSortAt(), cursorPayload.getReportId(), cursorPayload.getWorkType()), 2);
        var pestControl = workItem(WorkType.PEST_CONTROL, 1, this.reportId, this.farm, this.crop, FarmingCycleReportStatus.COMPLETED, day(5), finalHarvestId);
        when(queryRepository.searchWorkItems(expectedCondition)).thenReturn(new FarmingCycleReportQueryRepository.WorkItemSearchResult(java.util.Arrays.asList(pestControl)));
        var matchingRecord = record("405", WorkType.PEST_CONTROL, 4, 0, this.farm, this.crop);
        var targetFinal = record("406", WorkType.HARVEST, 5, 0, this.farm, this.crop);
        when(sourceRepository.load(memberId, Set.of(farmId),
            Set.of(cropId))).thenReturn(new FarmingWorkReportSourceSnapshot(java.util.Arrays.asList(matchingRecord, targetFinal),
            Set.of(Objects.requireNonNull(targetFinal.getId())), Map.of()));
        var page = service.list(new FarmingWorkReportSearchCondition(memberId, Set.of(), Set.of(), java.util.Arrays.asList(WorkType.PEST_CONTROL), cursor, 1));
        assertThat(single(page.getItems()).getThumbnailUrl()).isNull();
        assertThat(page.getNextCursor()).isNull();
        verify(queryRepository).searchWorkItems(expectedCondition);
        verify(sourceRepository).load(memberId, Set.of(farmId), Set.of(cropId));
    }

    @Test
    @DisplayName("list rejects a cursor created before last worked ordering")
    void listRejectsACursorCreatedBeforeLastWorkedOrdering() throws Exception {
        var legacyCursor = cursorCodec.encode(new LegacyFarmingWorkReportCursorPayload(FarmingCycleReportStatus.ACTIVE, day(30), id("399"), WorkType.WATERING));
        assertThatThrownBy(() -> {
            service.list(new FarmingWorkReportSearchCondition(memberId, Set.of(), Set.of(), List.of(), legacyCursor, 1));
        }).isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_CURSOR);
        verifyNoInteractions(queryRepository, sourceRepository);
    }

    @Test
    @DisplayName("list isolates thumbnails by exact farm and crop scope from cross product records")
    void listIsolatesThumbnailsByExactFarmAndCropScopeFromCrossProductRecords() throws Exception {
        var otherFarm = new Farm(id("102"), member, "다른 농장", "서울시 서초구");
        var otherCrop = new Crop(id("202"), 423, "감초", CropUsePartCategory.ROOT_BARK);
        var firstFinalId = id("406");
        var secondFinalId = id("416");
        var firstItem = workItem(WorkType.WATERING, 1, id("301"), farm, crop, FarmingCycleReportStatus.COMPLETED, day(5), firstFinalId);
        var secondItem = workItem(WorkType.WATERING, 1, id("302"), otherFarm, otherCrop, FarmingCycleReportStatus.COMPLETED, day(5), secondFinalId);
        var expectedCondition = new FarmingCycleReportQueryRepository.WorkItemSearchCondition(memberId, Set.of(), Set.of(),
            java.util.Arrays.asList(WorkType.WATERING), null, 3);
        when(queryRepository.searchWorkItems(expectedCondition)).thenReturn(new FarmingCycleReportQueryRepository.WorkItemSearchResult(java.util.Arrays.asList(firstItem,
            secondItem)));
        var firstPictured = record("401", WorkType.WATERING, 1, 0, this.farm, this.crop);
        var secondPictured = record("411", WorkType.WATERING, 1, 0, otherFarm, otherCrop);
        var temptingCrossProduct = record("421", WorkType.WATERING, 2, 0, farm, otherCrop);
        var firstFinal = record("406", WorkType.HARVEST, 3, 0, this.farm, this.crop);
        var secondFinal = record("416", WorkType.HARVEST, 3, 0, otherFarm, otherCrop);
        var farmIds = Set.of(Objects.requireNonNull(farm.getId()), Objects.requireNonNull(otherFarm.getId()));
        var cropIds = Set.of(Objects.requireNonNull(crop.getId()), Objects.requireNonNull(otherCrop.getId()));
        when(sourceRepository.load(memberId, farmIds, cropIds)).thenReturn(new FarmingWorkReportSourceSnapshot(java.util.Arrays.asList(secondFinal,
            temptingCrossProduct, firstPictured, secondPictured, firstFinal), Set.of(Objects.requireNonNull(firstFinal.getId()),
            Objects.requireNonNull(secondFinal.getId())), Map.ofEntries(Map.entry(Objects.requireNonNull(firstPictured.getId()),
            "https://img/first-scope.jpg"), Map.entry(Objects.requireNonNull(secondPictured.getId()), "https://img/second-scope.jpg"),
            Map.entry(Objects.requireNonNull(temptingCrossProduct.getId()), "https://img/cross-product-newer.jpg"))));
        var page = service.list(new FarmingWorkReportSearchCondition(memberId, Set.of(), Set.of(), java.util.Arrays.asList(WorkType.WATERING), null, 2));
        assertThat(page.getItems().stream().map(it -> it.getThumbnailUrl()).toList()).containsExactly("https://img/first-scope.jpg", "https://img/second-scope.jpg");
        assertThat(page.getNextCursor()).isNull();
        verify(queryRepository).searchWorkItems(expectedCondition);
        verify(sourceRepository).load(memberId, farmIds, cropIds);
    }

    @Test
    @DisplayName("list resolves tied pictured records by created time then record id")
    void listResolvesTiedPicturedRecordsByCreatedTimeThenRecordId() throws Exception {
        var expectedCondition = new FarmingCycleReportQueryRepository.WorkItemSearchCondition(memberId, Set.of(farmId), Set.of(cropId),
            java.util.Arrays.asList(WorkType.WATERING), null, 2);
        var item = workItem(WorkType.WATERING, 3, this.reportId, this.farm, this.crop, FarmingCycleReportStatus.COMPLETED, day(5), finalHarvestId);
        when(queryRepository.searchWorkItems(expectedCondition)).thenReturn(new FarmingCycleReportQueryRepository.WorkItemSearchResult(java.util.Arrays.asList(item)));
        var higherIdButOlderCreated = record("499", WorkType.WATERING, 3, 1, this.farm, this.crop);
        var lowerTiedId = record("451", WorkType.WATERING, 3, 2, this.farm, this.crop);
        var higherTiedId = record("452", WorkType.WATERING, 3, 2, this.farm, this.crop);
        var targetFinal = record("406", WorkType.HARVEST, 5, 0, this.farm, this.crop);
        when(sourceRepository.load(memberId, Set.of(farmId),
            Set.of(cropId))).thenReturn(new FarmingWorkReportSourceSnapshot(java.util.Arrays.asList(targetFinal, higherTiedId, higherIdButOlderCreated,
            lowerTiedId), Set.of(Objects.requireNonNull(targetFinal.getId())),
            Map.ofEntries(Map.entry(Objects.requireNonNull(higherIdButOlderCreated.getId()), "https://img/higher-id-older-created.jpg"),
            Map.entry(Objects.requireNonNull(lowerTiedId.getId()), "https://img/lower-tied-id.jpg"),
            Map.entry(Objects.requireNonNull(higherTiedId.getId()), "https://img/higher-tied-id.jpg"))));
        var page = service.list(new FarmingWorkReportSearchCondition(memberId, Set.of(farmId), Set.of(cropId), java.util.Arrays.asList(WorkType.WATERING), null, 1));
        assertThat(single(page.getItems()).getThumbnailUrl()).isEqualTo("https://img/higher-tied-id.jpg");
        verify(queryRepository).searchWorkItems(expectedCondition);
        verify(sourceRepository).load(memberId, Set.of(farmId), Set.of(cropId));
    }

    @Test
    @DisplayName("empty projection still performs one fixed source load with empty scopes")
    void emptyProjectionStillPerformsOneFixedSourceLoadWithEmptyScopes() throws Exception {
        var expectedCondition = new FarmingCycleReportQueryRepository.WorkItemSearchCondition(memberId, Set.of(), Set.of(), List.of(), null, 11);
        when(queryRepository.searchWorkItems(expectedCondition)).thenReturn(new FarmingCycleReportQueryRepository.WorkItemSearchResult(List.of()));
        when(sourceRepository.load(memberId, Set.of(), Set.of())).thenReturn(new FarmingWorkReportSourceSnapshot(List.of(), Set.of(), Map.of()));
        var page = service.list(new FarmingWorkReportSearchCondition(memberId, Set.of(), Set.of(), List.of(), null, 10));
        assertThat(page.getItems()).isEmpty();
        assertThat(page.getNextCursor()).isNull();
        verify(queryRepository).searchWorkItems(expectedCondition);
        verify(sourceRepository).load(memberId, Set.of(), Set.of());
        verifyNoMoreInteractions(sourceRepository);
    }
    private FarmingCycleReportQueryRepository.WorkItem workItem(WorkType workType, int recordCount, UUID reportId, Farm farm, Crop crop,
        FarmingCycleReportStatus status, LocalDateTime endsAt, UUID finalHarvestRecordId) {
        return new FarmingCycleReportQueryRepository.WorkItem(reportId, status, Objects.requireNonNull(farm.getId()), farm.getName(),
            Objects.requireNonNull(crop.getId()), crop.getName(), day(1), endsAt, finalHarvestRecordId, workType, recordCount, LocalDate.of(2026, 1, 5));
    }
    private void assertDetailError(WorkType workType, ErrorCode expected) {
        assertThatThrownBy(() -> service.getDetail(memberId, reportId, workType)).isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(expected);
    }
    private FarmingCycleReport report(FarmingCycleReportStatus status, CycleReportStatistics statistics) {
        var sourceStatus = status == FarmingCycleReportStatus.ACTIVE ? FarmingCycleReportStatus.ACTIVE : FarmingCycleReportStatus.COMPLETED;
        var report = FarmingCycleReport.create(member, farm, crop, new FarmingCycleReportProjection(sourceStatus, day(1),
            sourceStatus == FarmingCycleReportStatus.COMPLETED ? day(5) : null, FarmingCycleStartBasis.FIRST_RECORD,
            sourceStatus == FarmingCycleReportStatus.COMPLETED ? record("406", WorkType.HARVEST, 5, 0, farm, crop) : null, 1, statistics));
        org.springframework.test.util.ReflectionTestUtils.setField(report, "id", reportId);
        if (status == FarmingCycleReportStatus.SUPERSEDED) report.supersede();
        return report;
    }
    private ReportFeedbackListResult feedbacks(ReportFeedbackDetailResult... feedbacks) {
        return new ReportFeedbackListResult(reportId, Arrays.asList(feedbacks));
    }
    private ReportFeedbackDetailResult feedback(WorkType workType, ReportFeedbackStatus status, ReportFeedbackResultContent content) {
        return new ReportFeedbackDetailResult(id("701"), workType, status, !Objects.equals(status, ReportFeedbackStatus.PENDING), (Objects.equals(status,
            ReportFeedbackStatus.FAILED) ? "STRUCTURED_OUTPUT_INVALID" : null), content, day(6), day(6));
    }
    private FarmingRecord record(String suffix, WorkType workType, long day, long createdMinute, Farm farm, Crop crop) {
        var record = new FarmingRecord(id(suffix), member, farm, crop, workType, day(day), "맑음", 20, "memo", EntryMode.MANUAL, false);
        setCreatedAt(record, day(day).plusMinutes(createdMinute));
        return record;
    }
    private void setCreatedAt(BaseTimeEntity entity, LocalDateTime createdAt) {
        org.springframework.test.util.ReflectionTestUtils.setField(entity, "createdAt", createdAt);
    }
    private LocalDateTime day(long day) {
        return baseTime.plusDays(day);
    }
    private UUID id(String suffix) { return UUID.fromString("00000000-0000-0000-0000-000000000" + suffix); }
    private record LegacyFarmingWorkReportCursorPayload(FarmingCycleReportStatus status, LocalDateTime sortAt, UUID reportId, WorkType workType) {}

    @Test
    void cursorEncodingPreservesTheOriginalJacksonFailure() throws Exception {
        var codec = org.mockito.Mockito.mock(OpaqueCursorCodec.class);
        var failure = new com.fasterxml.jackson.core.JsonProcessingException("fixture encoding failure") {};
        when(codec.encode(org.mockito.ArgumentMatchers.any())).thenThrow(failure);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "cursorCodec", codec);
        var row = workItem(WorkType.WATERING, 1, reportId, farm, crop, FarmingCycleReportStatus.COMPLETED, day(5), finalHarvestId);
        when(queryRepository.searchWorkItems(org.mockito.ArgumentMatchers.any())).thenReturn(
            new FarmingCycleReportQueryRepository.WorkItemSearchResult(List.of(row, row)));
        when(sourceRepository.load(memberId, Set.of(farmId), Set.of(cropId))).thenReturn(
            new FarmingWorkReportSourceSnapshot(List.of(), Set.of(), Map.of()));
        var thrown = org.junit.jupiter.api.Assertions.assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,
            () -> service.list(new FarmingWorkReportSearchCondition(memberId, Set.of(), Set.of(), List.of(), null, 1)));
        org.junit.jupiter.api.Assertions.assertSame(failure, thrown);
    }

    @Test
    void workListRejectsJsonNullCursorBeforeQueryingReports() {
        var cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("null".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var thrown = org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class,
            () -> service.list(new FarmingWorkReportSearchCondition(memberId, Set.of(), Set.of(), List.of(), cursor, 20)));
        org.junit.jupiter.api.Assertions.assertEquals(ErrorCode.INVALID_CURSOR, thrown.getErrorCode());
        verifyNoInteractions(queryRepository, sourceRepository);
    }

    private static <T> T single(List<T> values) {
        assertThat(values).hasSize(1);
        return values.getFirst();
    }

}
