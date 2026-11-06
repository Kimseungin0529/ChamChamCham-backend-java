package com.chamchamcham.api.report.controller;

import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.api.report.dto.FarmingCycleReportResponses;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.report.FarmingCycleReportQueryService;
import com.chamchamcham.application.report.FarmingCycleReportResult;
import com.chamchamcham.application.report.FarmingCycleReportSearchCondition;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.report.Coverage;
import com.chamchamcham.domain.report.AmountByUnit;
import com.chamchamcham.domain.report.CategoryAmountByUnit;
import com.chamchamcham.domain.report.CategoryMethodStatistics;
import com.chamchamcham.domain.report.CommonOnlyStatistics;
import com.chamchamcham.domain.report.CountDistribution;
import com.chamchamcham.domain.report.CycleReportStatistics;
import com.chamchamcham.domain.report.FertilizingStatistics;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import com.chamchamcham.domain.report.HarvestStatistics;
import com.chamchamcham.domain.report.HarvestPartStatistics;
import com.chamchamcham.domain.report.GrowthPeriodRange;
import com.chamchamcham.domain.report.MaterialCategoryStatistics;
import com.chamchamcham.domain.report.PestControlStatistics;
import com.chamchamcham.domain.report.PlantingStatistics;
import com.chamchamcham.domain.report.PropagationStatistics;
import com.chamchamcham.domain.report.TargetCount;
import com.chamchamcham.domain.report.WateringStatistics;
import com.chamchamcham.domain.report.WeedingStatistics;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@WebMvcTest(FarmingCycleReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FarmingCycleReportControllerTest {
    @Autowired private MockMvc mockMvc;
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID farmId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID secondFarmId = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private final UUID secondCropId = UUID.fromString("00000000-0000-0000-0000-000000000302");
    private final UUID selectedReportId = UUID.fromString("00000000-0000-0000-0000-000000000601");
    private final UUID previousReportId = UUID.fromString("00000000-0000-0000-0000-000000000602");
    private final UUID finalHarvestRecordId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    @MockBean private FarmingCycleReportQueryService service;
    @MockBean private TokenProvider tokenProvider;
    @Test
    @DisplayName("former current path is rejected as an invalid report id")
    void former_current_path_is_rejected_as_an_invalid_report_id() throws Exception {
        mockMvc.perform(get("/api/v1/farming-reports/current")
            .with(authenticatedMember(memberId.toString()))
            .param("farmId", farmId.toString())
            .param("cropId", cropId.toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("list returns completed metadata and cursor without statistics")
    void list_returns_completed_metadata_and_cursor_without_statistics() throws Exception {
        when(service.listCompleted(searchCondition(this.farmId, this.cropId, null, 20)))
            .thenReturn(completedPage());
        mockMvc.perform(get("/api/v1/farming-reports")
            .with(authenticatedMember(memberId.toString()))
            .param("farmId", farmId.toString())
            .param("cropId", cropId.toString())
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].status", equalTo("COMPLETED")))
            .andExpect(jsonPath("$.data.items[0].statistics").doesNotExist())
            .andExpect(jsonPath("$.data.nextCursor", equalTo("next-cursor")));
    }

    @Test
    @DisplayName("detail returns selected full typed statistics without comparison snapshots")
    void detail_returns_selected_full_typed_statistics_without_comparison_snapshots() throws Exception {
        when(service.getDetail(memberId, selectedReportId))
            .thenReturn(new FarmingCycleReportResult.Detail(selectedSnapshot(representativeStatistics())));
        mockMvc.perform(get("/api/v1/farming-reports/{reportId}", selectedReportId)
            .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.selected.id", equalTo(selectedReportId.toString())))
            .andExpect(jsonPath("$.data.selected.statistics.planting.recordCount", equalTo(1)))
            .andExpect(jsonPath("$.data.selected.statistics.planting.plantingMethodDistribution[0].code", equalTo("SEED")))
            .andExpect(jsonPath("$.data.selected.statistics.planting.plantingMethodDistribution[0].ratePct", equalTo(100.0)))
            .andExpect(jsonPath("$.data.selected.statistics.watering.recordCount", equalTo(2)))
            .andExpect(jsonPath("$.data.selected.statistics.pestControl.totalSprayAmountMl", equalTo(2000.0)))
            .andExpect(jsonPath("$.data.selected.statistics.pestControl.totalSprayAmountLiters").doesNotExist())
            .andExpect(jsonPath("$.data.selected.statistics.harvest.totalAmountKg", equalTo(30.0)))
            .andExpect(jsonPath("$.data.selected.statistics.harvest.growthPeriodDistribution[0].code", equalTo("6")))
            .andExpect(jsonPath("$.data.selected.statistics.harvest.amountCoverage.recordedCount", equalTo(2)))
            .andExpect(jsonPath("$.data.selected.statistics.harvest.amountCoverage.targetCount", equalTo(3)))
            .andExpect(jsonPath("$.data.previous").doesNotExist())
            .andExpect(jsonPath("$.data.comparison").doesNotExist());
        verify(service).getDetail(memberId, selectedReportId);
    }

    @Test
    @DisplayName("former current path without query parameters is rejected as invalid input")
    void former_current_path_without_query_parameters_is_rejected_as_invalid_input() throws Exception {
        mockMvc.perform(get("/api/v1/farming-reports/current")
            .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("list rejects size above boundary before service call")
    void list_rejects_size_above_boundary_before_service_call() throws Exception {
        mockMvc.perform(get("/api/v1/farming-reports")
            .with(authenticatedMember(memberId.toString()))
            .param("farmId", farmId.toString())
            .param("cropId", cropId.toString())
            .param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("detail maps report not found to not found")
    void detail_maps_report_not_found_to_not_found() throws Exception {
        when(service.getDetail(memberId, selectedReportId))
            .thenThrow(new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        mockMvc.perform(get("/api/v1/farming-reports/{reportId}", selectedReportId)
            .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code", equalTo("REPORT_001")));
        verify(service).getDetail(memberId, selectedReportId);
    }

    @Test
    @DisplayName("list passes cursor and requested size to service")
    void list_passes_cursor_and_requested_size_to_service() throws Exception {
        when(service.listCompleted(searchCondition(this.farmId, this.cropId, "cursor-1", 5)))
            .thenReturn(new FarmingCycleReportResult.Page(java.util.Collections.emptyList(), null));
        mockMvc.perform(get("/api/v1/farming-reports")
            .with(authenticatedMember(memberId.toString()))
            .param("farmId", farmId.toString())
            .param("cropId", cropId.toString())
            .param("cursor", "cursor-1")
            .param("size", "5"))
            .andExpect(status().isOk());
        verify(service).listCompleted(searchCondition(this.farmId, this.cropId, "cursor-1", 5));
    }

    @Test
    @DisplayName("list translates an invalid cursor from the query service to invalid input")
    void list_translates_an_invalid_cursor_from_the_query_service_to_invalid_input() throws Exception {
        var condition = searchCondition(null, null, "not-base64", 20);
        when(service.listCompleted(condition))
            .thenThrow(new BusinessException(ErrorCode.INVALID_CURSOR));
        mockMvc.perform(get("/api/v1/farming-reports")
            .with(authenticatedMember(memberId.toString()))
            .param("cursor", "not-base64"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
        verify(service).listCompleted(condition);
    }

    @Test
    @DisplayName("list accepts no filters either filter and both filters")
    void list_accepts_no_filters_either_filter_and_both_filters() throws Exception {
        var conditions = java.util.Arrays.asList(searchCondition(null,
            null,
            null,
            20),
            searchCondition(farmId,
            null,
            null,
            20),
            searchCondition(null,
            cropId,
            null,
            20),
            searchCondition(farmId,
            cropId,
            null,
            20));
        for (var condition : conditions) {
            when(service.listCompleted(condition))
                .thenReturn(new FarmingCycleReportResult.Page(java.util.Collections.emptyList(), null));
        };
        mockMvc.perform(get("/api/v1/farming-reports").with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/farming-reports")
            .with(authenticatedMember(memberId.toString()))
            .param("farmId", farmId.toString())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/farming-reports")
            .with(authenticatedMember(memberId.toString()))
            .param("cropId", cropId.toString())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/farming-reports")
            .with(authenticatedMember(memberId.toString()))
            .param("farmId", farmId.toString())
            .param("cropId", cropId.toString())).andExpect(status().isOk());
        for (var condition : conditions) { verify(service).listCompleted(condition); };
    }

    @Test
    @DisplayName("list binds repeated farm and crop ids as multi filters")
    void list_binds_repeated_farm_and_crop_ids_as_multi_filters() throws Exception {
        var condition = new FarmingCycleReportSearchCondition(memberId,
            new java.util.LinkedHashSet<>(java.util.Arrays.asList(farmId,
            secondFarmId)),
            new java.util.LinkedHashSet<>(java.util.Arrays.asList(cropId,
            secondCropId)),
            null,
            20);
        when(service.listCompleted(condition)).thenReturn(new FarmingCycleReportResult.Page(java.util.Collections.emptyList(), null));
        mockMvc.perform(get("/api/v1/farming-reports")
            .with(authenticatedMember(memberId.toString()))
            .param("farmId", farmId.toString(), secondFarmId.toString())
            .param("cropId", cropId.toString(), secondCropId.toString())).andExpect(status().isOk());
        verify(service).listCompleted(condition);
    }

    @Test
    @DisplayName("snapshot response copies every nested statistic into API owned types")
    void snapshot_response_copies_every_nested_statistic_into_API_owned_types() throws Exception {
        var statistics = representativeStatistics();
        var response = FarmingCycleReportResponses.SnapshotResponse.from(selectedSnapshot(statistics));
        assertThat(response.getStatistics()).isNotSameAs(statistics);
        assertThat(response.getStatistics()).isNotInstanceOf(CycleReportStatistics.class);
        assertThat(response.getStatistics())
            .usingRecursiveComparison()
            .ignoringFields("pestControl.totalSprayAmountMl", "pestControl.totalSprayAmountLiters")
            .isEqualTo(statistics);
        assertThat(response.getStatistics().getPestControl().getTotalSprayAmountMl())
            .isEqualByComparingTo("2000.0000");
    }

    private FarmingCycleReportSearchCondition searchCondition(UUID farmId, UUID cropId, String cursor, int size) {
        return new FarmingCycleReportSearchCondition(memberId,
            farmId == null ? Collections.emptySet() : Collections.singleton(farmId),
            cropId == null ? Collections.emptySet() : Collections.singleton(cropId),
            cursor,
            size);
    }

    private FarmingCycleReportResult.Page completedPage() {
        return new FarmingCycleReportResult.Page(java.util.Arrays.asList(new FarmingCycleReportResult.Metadata(previousReportId,
            farmId,
            "초록농장",
            cropId,
            "감초",
            FarmingCycleReportStatus.COMPLETED,
            LocalDateTime.of(2026,
            1,
            1,
            9,
            0),
            LocalDateTime.of(2026,
            6,
            30,
            17,
            0),
            FarmingCycleStartBasis.FIRST_RECORD,
            7L)),
        "next-cursor");
    }

    private FarmingCycleReportResult.Snapshot selectedSnapshot(CycleReportStatistics statistics) {
        return snapshot(selectedReportId, FarmingCycleReportStatus.ACTIVE, LocalDateTime.of(2026, 7, 1, 9, 0), null, statistics);
    }

    private FarmingCycleReportResult.Snapshot completedSnapshot() {
        return snapshot(previousReportId,
            FarmingCycleReportStatus.COMPLETED,
            LocalDateTime.of(2026,
            1,
            1,
            9,
            0),
            LocalDateTime.of(2026,
            6,
            30,
            17,
            0),
            representativeStatistics());
    }

    private FarmingCycleReportResult.Snapshot snapshot(UUID id,
        FarmingCycleReportStatus status,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        CycleReportStatistics statistics) {
        return new FarmingCycleReportResult.Snapshot(id,
            farmId,
            "초록농장",
            cropId,
            "감초",
            status,
            startsAt,
            endsAt,
            FarmingCycleStartBasis.FIRST_RECORD,
            (status == FarmingCycleReportStatus.COMPLETED ? finalHarvestRecordId : null),
            1,
            7L,
            statistics);
    }

    private CycleReportStatistics representativeStatistics() {
        return new CycleReportStatistics(new PlantingStatistics(1,
            LocalDate.of(2026,
            1,
            2),
            LocalDate.of(2026,
            1,
            3),
            2,
            new BigDecimal("1.0"),
            1,
            new BigDecimal("100.0"),
            java.util.Arrays.asList(distribution("SUNNY",
        "맑음")),
            new BigDecimal("18.5"),
            java.util.Arrays.asList(distribution("SEED",
        "씨앗 심기")),
            java.util.Arrays.asList(new PropagationStatistics("SEED",
            "종자",
            1,
            new BigDecimal("100.0"),
            new BigDecimal("12.0"),
            "EA",
            new Coverage(1,
            1)))),
            new WateringStatistics(2,
            null,
            null,
            2,
            null,
            0,
            null,
            java.util.Arrays.asList(distribution("CLOUDY",
        "흐림")),
            null,
            java.util.Arrays.asList(distribution("SUFFICIENT",
        "충분")),
            java.util.Arrays.asList(distribution("SPRAYING",
        "살수"))),
            new FertilizingStatistics(2,
            null,
            null,
            0,
            null,
            0,
            null,
            java.util.Collections.emptyList(),
            null,
            new BigDecimal("8.0"),
            new BigDecimal("4.0"),
            new Coverage(2,
            2),
            java.util.Collections.emptyList(),
            java.util.Arrays.asList(new MaterialCategoryStatistics("ORGANIC",
            "유기질",
            2,
            new BigDecimal("100.0"),
            new BigDecimal("8.0"),
            new BigDecimal("100.0"))),
            java.util.Arrays.asList(distribution("SOIL",
        "토양")),
            java.util.Arrays.asList(new CategoryMethodStatistics("ORGANIC",
            "유기질",
            "SOIL",
            "토양",
            2,
            new BigDecimal("100.0")))),
            new PestControlStatistics(1,
            null,
            null,
            0,
            null,
            0,
            null,
            java.util.Collections.emptyList(),
            null,
            java.util.Arrays.asList(distribution("PESTICIDE",
        "농약")),
            java.util.Arrays.asList(new AmountByUnit("ML",
            new BigDecimal("20.0"),
            new Coverage(1,
            1))),
            java.util.Arrays.asList(new CategoryAmountByUnit("PESTICIDE",
            "농약",
            "ML",
            1,
            new BigDecimal("20.0"),
            new Coverage(1,
            1))),
            new BigDecimal("2.0"),
            new Coverage(1,
            1),
            java.util.Arrays.asList(new TargetCount("진딧물",
            1))),
            new WeedingStatistics(1,
            null,
            null,
            0,
            null,
            0,
            null,
            java.util.Collections.emptyList(),
            null,
            java.util.Arrays.asList(distribution("HAND",
        "손제초"))),
            new CommonOnlyStatistics(1,
            null,
            null,
            0,
            null,
            0,
            null,
            java.util.Arrays.asList(distribution("SUNNY",
        "맑음")),
            null),
            new HarvestStatistics(3,
            null,
            null,
            0,
            null,
            0,
            null,
            java.util.Collections.emptyList(),
            null,
            new BigDecimal("30.0"),
            new BigDecimal("10.0"),
            new Coverage(2,
            3),
            LocalDate.of(2026,
            6,
            1),
            LocalDate.of(2026,
            6,
            30),
            java.util.Arrays.asList(new HarvestPartStatistics("ROOT_BARK",
            "근피",
            3,
            new BigDecimal("100.0"),
            new BigDecimal("30.0"),
            new BigDecimal("100.0"),
            new Coverage(2,
            3))),
            6,
            new GrowthPeriodRange(5,
            7),
            java.util.Arrays.asList(distribution("6",
        "6개월"))),
            new CommonOnlyStatistics(1,
            null,
            null,
            0,
            null,
            1,
            new BigDecimal("100.0"),
            java.util.Collections.emptyList(),
            null));
    }

    private CountDistribution distribution(String code, String label) {
        return new CountDistribution(code, label, 1, new BigDecimal("100.0"));
    }

    private RequestPostProcessor authenticatedMember(String memberId) {
        return (RequestPostProcessor) (request -> {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(memberId,
                null,
                java.util.Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"))));
                return request;
        });
    }
}
