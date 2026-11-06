package com.chamchamcham.api.report.controller;

import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.application.coaching.reportfeedback.lifecycle.ReportFeedbackItemResult;
import com.chamchamcham.application.coaching.reportfeedback.lifecycle.ReportFeedbackResultContent;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.report.FarmingWorkReportQueryService;
import com.chamchamcham.application.report.FarmingWorkReportResult;
import com.chamchamcham.application.report.FarmingWorkReportSearchCondition;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackStatus;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.CommonOnlyStatistics;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.WateringStatistics;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@WebMvcTest(FarmingWorkReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FarmingWorkReportControllerTest {
    @Autowired private MockMvc mockMvc;
    private final UUID memberId = id("001");
    private final UUID farmId = id("101");
    private final UUID secondFarmId = id("102");
    private final UUID cropId = id("201");
    private final UUID secondCropId = id("202");
    private final UUID reportId = id("301");
    private final UUID finalHarvestRecordId = id("401");
    private final LocalDateTime startsAt = LocalDateTime.of(2026, 3, 1, 9, 0);
    private final LocalDateTime endsAt = LocalDateTime.of(2026, 8, 1, 9, 0);
    @MockBean private FarmingWorkReportQueryService service;
    @MockBean private TokenProvider tokenProvider;
    @Test
    @DisplayName("list binds optional filters cursor and size and returns work card shape")
    void list_binds_optional_filters_cursor_and_size_and_returns_work_card_shape() throws Exception {
        var condition = new FarmingWorkReportSearchCondition(memberId,
            new java.util.LinkedHashSet<>(java.util.Arrays.asList(farmId)),
            new java.util.LinkedHashSet<>(java.util.Arrays.asList(cropId)),
            java.util.Arrays.asList(WorkType.WATERING),
            "cursor-1",
            2);
        when(service.list(condition)).thenReturn(new FarmingWorkReportResult.Page(java.util.Arrays.asList(workItem(FarmingCycleReportStatus.COMPLETED,
            this.endsAt,
            this.finalHarvestRecordId)),
        "cursor-2"));
        mockMvc.perform(get("/api/v1/farming-reports/work-items")
            .with(authenticatedMember(memberId.toString()))
            .param("farmId", farmId.toString())
            .param("cropId", cropId.toString())
            .param("workType", "WATERING")
            .param("cursor", "cursor-1")
            .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].reportId", equalTo(reportId.toString())))
            .andExpect(jsonPath("$.data.items[0].status", equalTo("COMPLETED")))
            .andExpect(jsonPath("$.data.items[0].workType", equalTo("WATERING")))
            .andExpect(jsonPath("$.data.items[0].workTypeLabel", equalTo("물 주기")))
            .andExpect(jsonPath("$.data.items[0].farmName", equalTo("약초농장")))
            .andExpect(jsonPath("$.data.items[0].cropName", equalTo("황기")))
            .andExpect(jsonPath("$.data.items[0].recordCount", equalTo(3)))
            .andExpect(jsonPath("$.data.items[0].thumbnailUrl", nullValue()))
            .andExpect(jsonPath("$.data.items[0].finalHarvestRecordId").doesNotExist())
            .andExpect(jsonPath("$.data.nextCursor", equalTo("cursor-2")));
        verify(service).list(condition);
    }

    @Test
    @DisplayName("list binds multiple work type filters")
    void list_binds_multiple_work_type_filters() throws Exception {
        var condition = new FarmingWorkReportSearchCondition(memberId,
            new java.util.LinkedHashSet<>(java.util.Arrays.asList(farmId)),
            new java.util.LinkedHashSet<>(java.util.Arrays.asList(cropId)),
            java.util.Arrays.asList(WorkType.WATERING,
            WorkType.HARVEST),
            null,
            20);
        when(service.list(condition)).thenReturn(new FarmingWorkReportResult.Page(java.util.Collections.emptyList(), null));
        mockMvc.perform(get("/api/v1/farming-reports/work-items")
            .with(authenticatedMember(memberId.toString()))
            .param("farmId", farmId.toString())
            .param("cropId", cropId.toString())
            .param("workType", "WATERING", "HARVEST"))
            .andExpect(status().isOk());
        verify(service).list(condition);
    }

    @Test
    @DisplayName("list returns active work card with nullable end date")
    void list_returns_active_work_card_with_nullable_end_date() throws Exception {
        var condition = new FarmingWorkReportSearchCondition(memberId,
            java.util.Collections.emptySet(),
            java.util.Collections.emptySet(),
            java.util.Collections.emptyList(),
            null,
            20);
        when(service.list(condition)).thenReturn(new FarmingWorkReportResult.Page(java.util.Arrays.asList(workItem(FarmingCycleReportStatus.ACTIVE,
            null,
            null)),
            null));
        mockMvc.perform(get("/api/v1/farming-reports/work-items")
            .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].status", equalTo("ACTIVE")))
            .andExpect(jsonPath("$.data.items[0].endsAt", nullValue()))
            .andExpect(jsonPath("$.data.items[0].thumbnailUrl", nullValue()));
    }

    @Test
    @DisplayName("list accepts omitted filters with default size")
    void list_accepts_omitted_filters_with_default_size() throws Exception {
        var condition = new FarmingWorkReportSearchCondition(memberId,
            java.util.Collections.emptySet(),
            java.util.Collections.emptySet(),
            java.util.Collections.emptyList(),
            null,
            20);
        when(service.list(condition)).thenReturn(new FarmingWorkReportResult.Page(java.util.Collections.emptyList(), null));
        mockMvc.perform(get("/api/v1/farming-reports/work-items")
            .with(authenticatedMember(memberId.toString()))).andExpect(status().isOk());
        verify(service).list(condition);
    }

    @Test
    @DisplayName("list binds repeated farm and crop ids as multi filters")
    void list_binds_repeated_farm_and_crop_ids_as_multi_filters() throws Exception {
        var condition = new FarmingWorkReportSearchCondition(memberId,
            new java.util.LinkedHashSet<>(java.util.Arrays.asList(farmId,
            secondFarmId)),
            new java.util.LinkedHashSet<>(java.util.Arrays.asList(cropId,
            secondCropId)),
            java.util.Collections.emptyList(),
            null,
            20);
        when(service.list(condition)).thenReturn(new FarmingWorkReportResult.Page(java.util.Collections.emptyList(), null));
        mockMvc.perform(get("/api/v1/farming-reports/work-items")
            .with(authenticatedMember(memberId.toString()))
            .param("farmId", farmId.toString(), secondFarmId.toString())
            .param("cropId", cropId.toString(), secondCropId.toString())).andExpect(status().isOk());
        verify(service).list(condition);
    }

    @Test
    @DisplayName("list rejects sizes outside one through one hundred")
    void list_rejects_sizes_outside_one_through_one_hundred() throws Exception {
        for (var size : java.util.Arrays.asList("0", "101")) {
            mockMvc.perform(get("/api/v1/farming-reports/work-items")
                .with(authenticatedMember(memberId.toString()))
                .param("size", size))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
        };
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("list rejects an unknown work type at the API boundary")
    void list_rejects_an_unknown_work_type_at_the_API_boundary() throws Exception {
        mockMvc.perform(get("/api/v1/farming-reports/work-items")
            .with(authenticatedMember(memberId.toString()))
            .param("workType", "IRRIGATION"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("list translates an invalid cursor from the query service to invalid input")
    void list_translates_an_invalid_cursor_from_the_query_service_to_invalid_input() throws Exception {
        var condition = new FarmingWorkReportSearchCondition(memberId,
            java.util.Collections.emptySet(),
            java.util.Collections.emptySet(),
            java.util.Collections.emptyList(),
            "not-base64",
            20);
        when(service.list(condition))
            .thenThrow(new BusinessException(ErrorCode.INVALID_CURSOR));
        mockMvc.perform(get("/api/v1/farming-reports/work-items")
            .with(authenticatedMember(memberId.toString()))
            .param("cursor", "not-base64"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
        verify(service).list(condition);
    }

    @Test
    @DisplayName("detail exposes common and only the selected typed statistics branch")
    void detail_exposes_common_and_only_the_selected_typed_statistics_branch() throws Exception {
        when(service.getDetail(memberId,
            reportId,
            WorkType.WATERING)).thenReturn(detail(WorkType.WATERING,
            "물 주기",
            new FarmingWorkReportResult.WorkStatistics(common(3),
            null,
            new WateringStatistics(3,
            null,
            null,
            2,
            null,
            0,
            null,
            java.util.Collections.emptyList(),
            null,
            java.util.Collections.emptyList(),
            java.util.Collections.emptyList()),
            null,
            null,
            null,
            null),
            FarmingCycleReportStatus.COMPLETED,
            this.endsAt,
            readyFeedback()));
        mockMvc.perform(get("/api/v1/farming-reports/{reportId}/work-types/{workType}",
            reportId,
        "WATERING").with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", equalTo("COMPLETED")))
            .andExpect(jsonPath("$.data.workType", equalTo("WATERING")))
            .andExpect(jsonPath("$.data.workTypeLabel", equalTo("물 주기")))
            .andExpect(jsonPath("$.data.statistics.common.recordCount", equalTo(3)))
            .andExpect(jsonPath("$.data.statistics.watering.recordCount", equalTo(3)))
            .andExpect(jsonPath("$.data.statistics.planting").doesNotExist())
            .andExpect(jsonPath("$.data.statistics.fertilizing").doesNotExist())
            .andExpect(jsonPath("$.data.statistics.pestControl").doesNotExist())
            .andExpect(jsonPath("$.data.statistics.weeding").doesNotExist())
            .andExpect(jsonPath("$.data.statistics.harvest").doesNotExist())
            .andExpect(jsonPath("$.data.feedback.status", equalTo("READY")))
            .andExpect(jsonPath("$.data.feedback.content.summary", equalTo("물 주기를 잘 이어갔어요.")))
            .andExpect(jsonPath("$.data.feedback.content.comparisons").isArray())
            .andExpect(jsonPath("$.data.feedback.content.comparisons[0].text", equalTo("직전 재배보다 물 주기 기록이 한 번 늘었어요.")))
            .andExpect(jsonPath("$.data.feedback.content.strengths[0].text", equalTo("흙 상태를 살폈어요.")));
        verify(service).getDetail(memberId, reportId, WorkType.WATERING);
    }

    @Test
    @DisplayName("detail returns active statistics with null feedback")
    void detail_returns_active_statistics_with_null_feedback() throws Exception {
        when(service.getDetail(memberId,
            reportId,
            WorkType.WATERING)).thenReturn(detail(WorkType.WATERING,
            "물 주기",
            new FarmingWorkReportResult.WorkStatistics(common(3),
            null,
            new WateringStatistics(3,
            null,
            null,
            0,
            null,
            0,
            null,
            java.util.Collections.emptyList(),
            null,
            java.util.Collections.emptyList(),
            java.util.Collections.emptyList()),
            null,
            null,
            null,
            null),
            FarmingCycleReportStatus.ACTIVE,
            null,
            null));
        mockMvc.perform(get("/api/v1/farming-reports/{reportId}/work-types/{workType}",
            reportId,
        "WATERING").with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", equalTo("ACTIVE")))
            .andExpect(jsonPath("$.data.endsAt", nullValue()))
            .andExpect(jsonPath("$.data.statistics.common.recordCount", equalTo(3)))
            .andExpect(jsonPath("$.data.feedback", nullValue()));
    }

    @Test
    @DisplayName("detail exposes common only work without a typed branch")
    void detail_exposes_common_only_work_without_a_typed_branch() throws Exception {
        when(service.getDetail(memberId,
            reportId,
            WorkType.PRUNING)).thenReturn(detail(WorkType.PRUNING,
            "가지 정리",
            new FarmingWorkReportResult.WorkStatistics(common(2),
            null,
            null,
            null,
            null,
            null,
            null),
            FarmingCycleReportStatus.COMPLETED,
            this.endsAt,
            new FarmingWorkReportResult.FeedbackStatus(ReportFeedbackStatus.PENDING,
            null)));
        mockMvc.perform(get("/api/v1/farming-reports/{reportId}/work-types/{workType}",
            reportId,
        "PRUNING").with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.workTypeLabel", equalTo("가지 정리")))
            .andExpect(jsonPath("$.data.statistics.common.recordCount", equalTo(2)))
            .andExpect(jsonPath("$.data.statistics.planting").doesNotExist())
            .andExpect(jsonPath("$.data.statistics.watering").doesNotExist())
            .andExpect(jsonPath("$.data.statistics.fertilizing").doesNotExist())
            .andExpect(jsonPath("$.data.statistics.pestControl").doesNotExist())
            .andExpect(jsonPath("$.data.statistics.weeding").doesNotExist())
            .andExpect(jsonPath("$.data.statistics.harvest").doesNotExist());
    }

    @Test
    @DisplayName("detail keeps feedback content null for pending and failed statuses")
    void detail_keeps_feedback_content_null_for_pending_and_failed_statuses() throws Exception {
        for (var feedbackStatus : java.util.Arrays.asList(ReportFeedbackStatus.PENDING, ReportFeedbackStatus.FAILED)) {
            when(service.getDetail(memberId,
                reportId,
                WorkType.WATERING)).thenReturn(detail(WorkType.WATERING,
                "물 주기",
                new FarmingWorkReportResult.WorkStatistics(common(3),
                null,
                new WateringStatistics(3,
                null,
                null,
                0,
                null,
                0,
                null,
                java.util.Collections.emptyList(),
                null,
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList()),
                null,
                null,
                null,
                null),
                FarmingCycleReportStatus.COMPLETED,
                this.endsAt,
                new FarmingWorkReportResult.FeedbackStatus(feedbackStatus,
                null)));
            mockMvc.perform(get("/api/v1/farming-reports/{reportId}/work-types/{workType}",
                reportId,
            "WATERING").with(authenticatedMember(memberId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedback.status", equalTo(feedbackStatus.name())))
                .andExpect(jsonPath("$.data.feedback.content", nullValue()));
        };
    }

    @Test
    @DisplayName("detail rejects an unknown path work type before service call")
    void detail_rejects_an_unknown_path_work_type_before_service_call() throws Exception {
        mockMvc.perform(get("/api/v1/farming-reports/{reportId}/work-types/{workType}",
            reportId,
        "IRRIGATION").with(authenticatedMember(memberId.toString())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("detail maps missing work to report scoped not found error")
    void detail_maps_missing_work_to_report_scoped_not_found_error() throws Exception {
        when(service.getDetail(memberId, reportId, WorkType.WATERING))
            .thenThrow(new BusinessException(ErrorCode.WORK_REPORT_NOT_FOUND));
        mockMvc.perform(get("/api/v1/farming-reports/{reportId}/work-types/{workType}",
            reportId,
        "WATERING").with(authenticatedMember(memberId.toString())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code", equalTo("REPORT_002")));
    }

    private FarmingWorkReportResult.Item workItem(FarmingCycleReportStatus status, LocalDateTime endsAt, UUID finalHarvestRecordId) {
        return new FarmingWorkReportResult.Item(reportId,
            status,
            farmId,
            "약초농장",
            cropId,
            "황기",
            startsAt,
            endsAt,
            finalHarvestRecordId,
            WorkType.WATERING,
            "물 주기",
            3,
            LocalDate.of(2026,
            7,
            30),
            null);
    }

    private FarmingWorkReportResult.Detail detail(WorkType workType,
        String workTypeLabel,
        FarmingWorkReportResult.WorkStatistics statistics,
        FarmingCycleReportStatus status,
        LocalDateTime endsAt,
        FarmingWorkReportResult.FeedbackStatus feedback) {
        return new FarmingWorkReportResult.Detail(reportId,
            status,
            workType,
            workTypeLabel,
            farmId,
            "약초농장",
            cropId,
            "황기",
            startsAt,
            endsAt,
            statistics,
            feedback);
    }

    private FarmingWorkReportResult.FeedbackStatus readyFeedback() {
        return new FarmingWorkReportResult.FeedbackStatus(ReportFeedbackStatus.READY,
            new ReportFeedbackResultContent("물 주기를 잘 이어갔어요.",
            java.util.Arrays.asList(new ReportFeedbackItemResult("흙 상태를 살폈어요.")),
            java.util.Collections.emptyList(),
            java.util.Arrays.asList(new ReportFeedbackItemResult("내일 흙을 확인하세요.")),
            java.util.Arrays.asList(new ReportFeedbackItemResult("직전 재배보다 물 주기 기록이 한 번 늘었어요."))));
    }

    private CommonOnlyStatistics common(int recordCount) {
        return new CommonOnlyStatistics(recordCount, null, null, recordCount, null, 0, null, java.util.Collections.emptyList(), null);
    }

    private RequestPostProcessor authenticatedMember(String id) {
        return (RequestPostProcessor) (request -> {
                SecurityContextHolder.getContext().setAuthentication( new UsernamePasswordAuthenticationToken(id,
                null,
                java.util.Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"))));
                return request;
        });
    }

    private UUID id(String suffix) {
        return UUID.fromString("00000000-0000-0000-0000-000000000%s".formatted(suffix));
    }
}
