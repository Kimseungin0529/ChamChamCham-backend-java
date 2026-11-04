package com.chamchamcham.application.report;

import com.chamchamcham.domain.report.WeedingStatistics;
import com.chamchamcham.domain.report.PlantingStatistics;
import com.chamchamcham.domain.report.FertilizingStatistics;
import com.chamchamcham.domain.report.HarvestStatistics;
import com.chamchamcham.domain.report.PestControlStatistics;
import com.chamchamcham.domain.report.CommonOnlyStatistics;
import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.crop.MemberCropRepository;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.report.CycleReportStatistics;
import com.chamchamcham.domain.report.FarmingCycleReport;
import com.chamchamcham.domain.report.FarmingCycleReportProjection;
import com.chamchamcham.domain.report.FarmingCycleReportQueryRepository;
import com.chamchamcham.domain.report.FarmingCycleReportRepository;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import com.chamchamcham.domain.report.WateringStatistics;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import com.chamchamcham.domain.farming.EntryMode;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(MockitoExtension.class)
class FarmingCycleReportQueryServiceTest {
    private final UUID memberId = uuid("000000000001");
    private final UUID farmId = uuid("000000000101");
    private final UUID secondFarmId = uuid("000000000102");
    private final UUID cropId = uuid("000000000201");
    private final UUID secondCropId = uuid("000000000202");
    private final UUID reportId = uuid("000000000301");
    private final UUID activeReportId = uuid("000000000302");
    private final UUID previousReportId = uuid("000000000303");
    private final UUID finalHarvestRecordId = uuid("000000000401");
    private final UUID previousFinalHarvestRecordId = uuid("000000000402");
    private final LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 9, 0);
    private final OpaqueCursorCodec cursorCodec = new OpaqueCursorCodec();
    @Mock private FarmRepository farmRepository;
    @Mock private MemberCropRepository memberCropRepository;
    @Mock private FarmingCycleReportRepository reportRepository;
    @Mock private FarmingCycleReportQueryRepository queryRepository;
    private FarmingCycleReportQueryService service;
    private Member member;
    private Farm farm;
    private Crop crop;
    private FarmingCycleReport active;
    private FarmingCycleReport selectedCompleted;
    private FarmingCycleReport previous;
    private FarmingCycleReport superseded;
    private CycleReportStatistics typedStatistics;

    @BeforeEach
    void setUp() throws Exception {
        service = new FarmingCycleReportQueryService(farmRepository, memberCropRepository, reportRepository, queryRepository, cursorCodec);
        member = new Member(memberId, "member@example.com", null);
        farm = new Farm(farmId, member, "약초농장", "서울시 강남구");
        crop = new Crop(cropId, 422, "황기", CropUsePartCategory.ROOT_BARK);
        typedStatistics = new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(1, null, null, 0, null, 0, null, List.of(), null,
            List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(),
            HarvestStatistics.empty(), CommonOnlyStatistics.empty());
        active = activeReport(activeReportId);
        selectedCompleted = completedReport(reportId, finalHarvestRecordId, day(30));
        previous = completedReport(previousReportId, previousFinalHarvestRecordId, day(20));
        superseded = completedReport(uuid("000000000304"), uuid("000000000403"), day(10));
        superseded.supersede();
    }

    @Test
    @DisplayName("detail returns only the selected completed report")
    void detailReturnsOnlyTheSelectedCompletedReport() throws Exception {
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(selectedCompleted);
        var result = service.getDetail(memberId, reportId);
        assertThat(result.getSelected().getId()).isEqualTo(reportId);
        assertThat(result.getSelected().getStatistics()).isSameAs(typedStatistics);
        verifyNoInteractions(queryRepository);
    }

    @Test
    @DisplayName("active detail returns selected without completed report lookup")
    void activeDetailReturnsSelectedWithoutCompletedReportLookup() throws Exception {
        when(reportRepository.findByIdAndMember_Id(activeReportId, memberId)).thenReturn(active);
        var result = service.getDetail(memberId, activeReportId);
        assertThat(result.getSelected().getId()).isEqualTo(activeReportId);
        verifyNoInteractions(queryRepository);
    }

    @Test
    @DisplayName("superseded detail is not exposed")
    void supersededDetailIsNotExposed() throws Exception {
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(superseded);
        var exception = assertThrows(BusinessException.class, () -> service.getDetail(memberId, reportId));
        assertEquals(ErrorCode.REPORT_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(queryRepository);
    }

    @Test
    @DisplayName("detail hides reports outside member scope")
    void detailHidesReportsOutsideMemberScope() throws Exception {
        when(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(null);
        var exception = assertThrows(BusinessException.class, () -> service.getDetail(memberId, reportId));
        assertEquals(ErrorCode.REPORT_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(queryRepository);
    }

    @Test
    @DisplayName("list completed uses lookahead and cursor from last visible boundary")
    void listCompletedUsesLookaheadAndCursorFromLastVisibleBoundary() throws Exception {
        stubScope();
        var newest = completedReport(uuid("000000000305"), uuid("000000000405"), day(40));
        when(queryRepository.searchCompleted(new FarmingCycleReportQueryRepository.SearchCondition(memberId, Set.of(farmId), Set.of(cropId), null,
            3))).thenReturn(new FarmingCycleReportQueryRepository.SearchResult(java.util.Arrays.asList(newest, selectedCompleted, previous)));
        var result = service.listCompleted(searchCondition(this.farmId, this.cropId, null, 2));
        assertThat(result.getItems().stream().map(it -> it.getId()).toList()).containsExactly(newest.getId(), selectedCompleted.getId());
        assertThat(result.getItems().getFirst()).hasNoNullFieldsOrPropertiesExcept();
        assertThat(result.getNextCursor()).isNotBlank();
        var decoded = cursorCodec.decode(result.getNextCursor(), FarmingCycleReportCursorPayload.class);
        assertEquals(selectedCompleted.getEndsAt(), decoded.getEndsAt());
        assertEquals(selectedCompleted.getFinalHarvestRecord().getId(), decoded.getFinalHarvestRecordId());
    }

    @Test
    @DisplayName("blank cursor is treated as absent")
    void blankCursorIsTreatedAsAbsent() throws Exception {
        stubScope();
        when(queryRepository.searchCompleted(new FarmingCycleReportQueryRepository.SearchCondition(memberId, Set.of(farmId), Set.of(cropId), null,
            11))).thenReturn(new FarmingCycleReportQueryRepository.SearchResult(List.of()));
        service.listCompleted(searchCondition(this.farmId, this.cropId, "   ", 10));
        verify(queryRepository).searchCompleted(new FarmingCycleReportQueryRepository.SearchCondition(memberId, Set.of(farmId), Set.of(cropId), null, 11));
    }

    @Test
    @DisplayName("invalid cursor is rejected")
    void invalidCursorIsRejected() throws Exception {
        stubScope();
        var exception = assertThrows(BusinessException.class, () -> service.listCompleted(searchCondition(this.farmId, this.cropId, "not-a-valid-cursor", 20)));
        assertEquals(ErrorCode.INVALID_CURSOR, exception.getErrorCode());
        verifyNoInteractions(queryRepository);
    }

    @Test
    @DisplayName("list rejects unowned farm before query lookup")
    void listRejectsUnownedFarmBeforeQueryLookup() throws Exception {
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(null);
        var exception = assertThrows(BusinessException.class, () -> service.listCompleted(searchCondition(this.farmId, this.cropId, null, 20)));
        assertEquals(ErrorCode.FARM_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(memberCropRepository);
        verifyNoInteractions(queryRepository);
    }

    @Test
    @DisplayName("list without filters stays member scoped without ownership lookups")
    void listWithoutFiltersStaysMemberScopedWithoutOwnershipLookups() throws Exception {
        when(queryRepository.searchCompleted(new FarmingCycleReportQueryRepository.SearchCondition(memberId, Set.of(), Set.of(), null,
            21))).thenReturn(new FarmingCycleReportQueryRepository.SearchResult(List.of()));
        var result = service.listCompleted(searchCondition(null, null, null, 20));
        assertThat(result.getItems()).isEmpty();
        verifyNoInteractions(farmRepository, memberCropRepository);
        verify(queryRepository).searchCompleted(new FarmingCycleReportQueryRepository.SearchCondition(memberId, Set.of(), Set.of(), null, 21));
    }

    @Test
    @DisplayName("list with crop only does not require a farm ownership lookup")
    void listWithCropOnlyDoesNotRequireAFarmOwnershipLookup() throws Exception {
        when(queryRepository.searchCompleted(new FarmingCycleReportQueryRepository.SearchCondition(memberId, Set.of(), Set.of(cropId), null,
            21))).thenReturn(new FarmingCycleReportQueryRepository.SearchResult(List.of()));
        service.listCompleted(searchCondition(null, cropId, null, 20));
        verifyNoInteractions(farmRepository, memberCropRepository);
    }

    @Test
    @DisplayName("list rejects crop that is not registered to member farm")
    void listRejectsCropThatIsNotRegisteredToMemberFarm() throws Exception {
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(memberCropRepository.existsByMemberIdAndFarmIdInAndCropId(memberId, Set.of(farmId), cropId)).thenReturn(false);
        var exception = assertThrows(BusinessException.class, () -> service.listCompleted(searchCondition(this.farmId, this.cropId, null, 20)));
        assertEquals(ErrorCode.CROP_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(queryRepository);
    }

    @Test
    @DisplayName("list validates and forwards multiple farm and crop filters")
    void listValidatesAndForwardsMultipleFarmAndCropFilters() throws Exception {
        var farmIds = Set.of(farmId, secondFarmId);
        var cropIds = Set.of(cropId, secondCropId);
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(farmRepository.findByIdAndOwnerId(secondFarmId, memberId)).thenReturn(farm);
        cropIds.forEach(selectedCropId -> {
            when(memberCropRepository.existsByMemberIdAndFarmIdInAndCropId(memberId, farmIds, selectedCropId)).thenReturn(true);
        });
        var expected = new FarmingCycleReportQueryRepository.SearchCondition(memberId, farmIds, cropIds, null, 21);
        when(queryRepository.searchCompleted(expected)).thenReturn(new FarmingCycleReportQueryRepository.SearchResult(List.of()));
        var result = service.listCompleted(new FarmingCycleReportSearchCondition(memberId, farmIds, cropIds, null, 20));
        assertThat(result.getItems()).isEmpty();
        verify(queryRepository).searchCompleted(expected);
    }
    private void stubScope() {
        when(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm);
        when(memberCropRepository.existsByMemberIdAndFarmIdInAndCropId(memberId, Set.of(farmId), cropId)).thenReturn(true);
    }
    private FarmingCycleReportSearchCondition searchCondition(UUID farmId, UUID cropId, String cursor, int size) {
        return new FarmingCycleReportSearchCondition(memberId, (farmId == null ? Set.of() : Set.of(farmId)), (cropId == null ? Set.of() : Set.of(cropId)), cursor, size);
    }
    private FarmingCycleReport activeReport(UUID id) {
        return report(id, new FarmingCycleReportProjection(FarmingCycleReportStatus.ACTIVE, day(40), null,
            FarmingCycleStartBasis.AFTER_PREVIOUS_FINAL_HARVEST, null, 1, typedStatistics));
    }
    private FarmingCycleReport completedReport(UUID id, UUID finalRecordId, LocalDateTime endedAt) {
        return report(id, new FarmingCycleReportProjection(FarmingCycleReportStatus.COMPLETED, endedAt.minusDays(10), endedAt,
            FarmingCycleStartBasis.FIRST_RECORD, finalHarvestRecord(finalRecordId, endedAt), 1, typedStatistics));
    }
    private FarmingCycleReport report(UUID id, FarmingCycleReportProjection projection) {
        var report = FarmingCycleReport.create(member, farm, crop, projection);
        org.springframework.test.util.ReflectionTestUtils.setField(report, "id", id);
        return report;
    }
    private FarmingRecord finalHarvestRecord(UUID id, LocalDateTime workedAt) {
        return new FarmingRecord(id, member, farm, crop, WorkType.HARVEST, workedAt, "맑음", 20, "최종 수확", com.chamchamcham.domain.farming.EntryMode.MANUAL, false);
    }
    private LocalDateTime day(long day) {
        return baseTime.plusDays(day);
    }
    private UUID uuid(String tail) {
        return UUID.fromString("00000000-0000-0000-0000-" + tail + "");
    }

    @Test
    void cursorEncodingPreservesTheOriginalJacksonFailure() throws Exception {
        var codec = org.mockito.Mockito.mock(OpaqueCursorCodec.class);
        var failure = new com.fasterxml.jackson.core.JsonProcessingException("fixture encoding failure") {};
        when(codec.encode(org.mockito.ArgumentMatchers.any())).thenThrow(failure);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "cursorCodec", codec);
        when(queryRepository.searchCompleted(org.mockito.ArgumentMatchers.any())).thenReturn(
            new FarmingCycleReportQueryRepository.SearchResult(List.of(selectedCompleted, selectedCompleted)));
        var thrown = assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,
            () -> service.listCompleted(new FarmingCycleReportSearchCondition(memberId, Set.of(), Set.of(), null, 1)));
        org.junit.jupiter.api.Assertions.assertSame(failure, thrown);
    }

    @Test
    void completedListRejectsJsonNullCursorBeforeQueryingReports() {
        var cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("null".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var thrown = assertThrows(BusinessException.class,
            () -> service.listCompleted(new FarmingCycleReportSearchCondition(memberId, Set.of(), Set.of(), cursor, 20)));
        org.junit.jupiter.api.Assertions.assertEquals(ErrorCode.INVALID_CURSOR, thrown.getErrorCode());
        verifyNoInteractions(queryRepository);
    }

}
