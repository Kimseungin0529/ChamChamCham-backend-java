package com.chamchamcham.api.coaching.reportfeedback.controller;

import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.chamchamcham.application.coaching.reportfeedback.lifecycle.*;
import com.chamchamcham.domain.coaching.reportfeedback.*;
import com.chamchamcham.domain.farming.WorkType;

@WebMvcTest(ReportFeedbackController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReportFeedbackControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TokenProvider tokenProvider;
    @MockBean private ReportFeedbackQueryService queryService;
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID reportId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID wateringFeedbackId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID fertilizingFeedbackId = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private final UUID harvestFeedbackId = UUID.fromString("00000000-0000-0000-0000-000000000203");

    @Test
    @DisplayName("report feedback returns an ordered work type collection with ready content only")
    void report_feedback_returns_an_ordered_work_type_collection_with_ready_content_only() throws Exception {
        when(queryService.get(memberId, reportId)).thenReturn(new ReportFeedbackListResult(reportId, List.of(
                feedback(wateringFeedbackId, WorkType.WATERING, ReportFeedbackStatus.READY, true, null,
                        new ReportFeedbackResultContent("물 주기 작업을 살펴봤어요.",
                                List.of(new ReportFeedbackItemResult("흙이 마른 정도를 꾸준히 확인했어요.")),
                                List.of(new ReportFeedbackItemResult("물을 준 간격도 함께 비교해 보세요.")),
                                List.of(new ReportFeedbackItemResult("내일 흙이 마른 정도를 다시 확인하세요.")),
                                List.of(new ReportFeedbackItemResult("직전 재배보다 물 주기 기록이 한 번 늘었어요.")))),
                feedback(fertilizingFeedbackId, WorkType.FERTILIZING, ReportFeedbackStatus.PENDING, false, null, null),
                feedback(harvestFeedbackId, WorkType.HARVEST, ReportFeedbackStatus.FAILED, true, "STRUCTURED_OUTPUT_INVALID", null))));
        mockMvc.perform(get("/api/v1/farming-reports/{reportId}/feedback", reportId)
                .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", equalTo(true)))
            .andExpect(jsonPath("$.data.reportId", equalTo(reportId.toString())))
            .andExpect(jsonPath("$.data.feedbacks.length()", equalTo(3)))
            .andExpect(jsonPath("$.data.feedbacks[0].feedbackId", equalTo(wateringFeedbackId.toString())))
            .andExpect(jsonPath("$.data.feedbacks[0].workType", equalTo("WATERING")))
            .andExpect(jsonPath("$.data.feedbacks[0].status", equalTo("READY")))
            .andExpect(jsonPath("$.data.feedbacks[0].inputPrepared", equalTo(true)))
            .andExpect(jsonPath("$.data.feedbacks[0].failureCode").isEmpty())
            .andExpect(jsonPath("$.data.feedbacks[0].feedback.summary", equalTo("물 주기 작업을 살펴봤어요.")))
            .andExpect(
                jsonPath(
                    "$.data.feedbacks[0].feedback.comparisons[0].text",
                    equalTo("직전 재배보다 물 주기 기록이 한 번 늘었어요.")))
            .andExpect(jsonPath("$.data.feedbacks[0].feedback.strengths[0].text", equalTo("흙이 마른 정도를 꾸준히 확인했어요.")))
            .andExpect(jsonPath("$.data.feedbacks[0].feedback.improvements[0].text", equalTo("물을 준 간격도 함께 비교해 보세요.")))
            .andExpect(jsonPath("$.data.feedbacks[0].feedback.nextActions[0].text", equalTo("내일 흙이 마른 정도를 다시 확인하세요.")))
            .andExpect(jsonPath("$.data.feedbacks[0].createdAt", equalTo("2026-07-14T10:00:00")))
            .andExpect(jsonPath("$.data.feedbacks[0].updatedAt", equalTo("2026-07-14T10:01:00")))
            .andExpect(jsonPath("$.data.feedbacks[0].feedback.strengths[0].basis").doesNotExist())
            .andExpect(jsonPath("$.data.feedbacks[0].feedback.comparisons[0].basis").doesNotExist())
            .andExpect(jsonPath("$.data.feedbacks[0].feedback.nextCycleActions").doesNotExist())
            .andExpect(jsonPath("$.data.feedbacks[0].feedback.citations").doesNotExist())
            .andExpect(jsonPath("$.data.feedbacks[0].citations").doesNotExist())
            .andExpect(jsonPath("$.data.feedbacks[1].workType", equalTo("FERTILIZING")))
            .andExpect(jsonPath("$.data.feedbacks[1].status", equalTo("PENDING")))
            .andExpect(jsonPath("$.data.feedbacks[1].inputPrepared", equalTo(false)))
            .andExpect(jsonPath("$.data.feedbacks[1].feedback").isEmpty())
            .andExpect(jsonPath("$.data.feedbacks[2].workType", equalTo("HARVEST")))
            .andExpect(jsonPath("$.data.feedbacks[2].status", equalTo("FAILED")))
            .andExpect(jsonPath("$.data.feedbacks[2].failureCode", equalTo("STRUCTURED_OUTPUT_INVALID")))
            .andExpect(jsonPath("$.data.feedbacks[2].feedback").isEmpty())
            .andExpect(jsonPath("$.data.status").doesNotExist())
            .andExpect(jsonPath("$.data.feedback").doesNotExist())
;
    }

    @Test
    @DisplayName("completed report without generated feedback returns an empty collection")
    void completed_report_without_generated_feedback_returns_an_empty_collection() throws Exception {
        when(queryService.get(memberId, reportId)).thenReturn(new ReportFeedbackListResult(reportId, List.of()));
        mockMvc.perform(get("/api/v1/farming-reports/{reportId}/feedback", reportId)
                .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reportId", equalTo(reportId.toString())))
            .andExpect(jsonPath("$.data.feedbacks").isArray())
            .andExpect(jsonPath("$.data.feedbacks").isEmpty());
    }

    @Test
    @DisplayName("report feedback without principal is unauthorized")
    void report_feedback_without_principal_is_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/farming-reports/{reportId}/feedback", reportId))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code", equalTo("AUTH_001")));
    }

    @Test
    @DisplayName("report feedback propagates report not found")
    void report_feedback_propagates_report_not_found() throws Exception {
        when(queryService.get(memberId, reportId)).thenThrow(new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        mockMvc.perform(get("/api/v1/farming-reports/{reportId}/feedback", reportId)
                .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code", equalTo("REPORT_001")));
    }

    @Test
    @DisplayName("failed work type feedback can be regenerated")
    void failed_work_type_feedback_can_be_regenerated() throws Exception {
        when(queryService.regenerate(memberId, reportId, WorkType.HARVEST)).thenReturn(
                feedback(harvestFeedbackId, WorkType.HARVEST, ReportFeedbackStatus.PENDING, false, null, null));
        mockMvc.perform(post("/api/v1/farming-reports/{reportId}/feedback/{workType}/regenerate", reportId, WorkType.HARVEST)
                .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.feedbackId", equalTo(harvestFeedbackId.toString())))
            .andExpect(jsonPath("$.data.workType", equalTo("HARVEST")))
            .andExpect(jsonPath("$.data.status", equalTo("PENDING")))
            .andExpect(jsonPath("$.data.inputPrepared", equalTo(false)))
            .andExpect(jsonPath("$.data.failureCode").isEmpty())
            .andExpect(jsonPath("$.data.feedback").isEmpty());
    }

    @Test
    @DisplayName("regeneration conflict is returned when work type feedback is not failed")
    void regeneration_conflict_is_returned_when_work_type_feedback_is_not_failed() throws Exception {
        when(queryService.regenerate(memberId, reportId, WorkType.WATERING)).thenThrow(new BusinessException(ErrorCode.REPORT_FEEDBACK_REGENERATION_NOT_ALLOWED));
        mockMvc.perform(post("/api/v1/farming-reports/{reportId}/feedback/{workType}/regenerate", reportId, WorkType.WATERING)
                .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code", equalTo("COACHING_004")));
    }

    private ReportFeedbackDetailResult feedback(UUID feedbackId, WorkType workType, ReportFeedbackStatus status,
                                                boolean inputPrepared, String failureCode, ReportFeedbackResultContent content) {
        return new ReportFeedbackDetailResult(feedbackId, workType, status, inputPrepared, failureCode, content,
                LocalDateTime.of(2026, 7, 14, 10, 0), LocalDateTime.of(2026, 7, 14, 10, 1));
    }

    private RequestPostProcessor authenticatedMember(String id) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    id, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            return request;
        };
    }

}
