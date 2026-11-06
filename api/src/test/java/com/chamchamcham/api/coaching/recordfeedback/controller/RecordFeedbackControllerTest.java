package com.chamchamcham.api.coaching.recordfeedback.controller;

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
import com.chamchamcham.application.coaching.recordfeedback.lifecycle.*;
import com.chamchamcham.domain.coaching.recordfeedback.*;

@WebMvcTest(RecordFeedbackController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RecordFeedbackControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TokenProvider tokenProvider;
    @MockBean private RecordFeedbackQueryService queryService;
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID recordId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID feedbackId = UUID.fromString("00000000-0000-0000-0000-000000000201");

    @Test
    @DisplayName("get status returns only phase two readiness fields")
    void get_status_returns_only_phase_two_readiness_fields() throws Exception {
        when(queryService.get(memberId, recordId)).thenReturn(result(RecordFeedbackStatus.PENDING, true, null, null));
        mockMvc.perform(get("/api/v1/farming-records/{recordId}/feedback", recordId)
                .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", equalTo(true)))
            .andExpect(jsonPath("$.data.feedbackId", equalTo(feedbackId.toString())))
            .andExpect(jsonPath("$.data.recordId", equalTo(recordId.toString())))
            .andExpect(jsonPath("$.data.status", equalTo("PENDING")))
            .andExpect(jsonPath("$.data.sourceRevision", equalTo(3)))
            .andExpect(jsonPath("$.data.inputPrepared", equalTo(true)))
            .andExpect(jsonPath("$.data.failureCode").isEmpty())
            .andExpect(jsonPath("$.data.feedback").isEmpty())
            .andExpect(jsonPath("$.data.inputSnapshot").doesNotExist());
    }

    @Test
    @DisplayName("ready response serializes feedback but not internal coaching fields")
    void ready_response_serializes_feedback_but_not_internal_coaching_fields() throws Exception {
        when(queryService.get(memberId, recordId)).thenReturn(result(RecordFeedbackStatus.READY, false, null,
                new RecordFeedbackResultContent("점적관수로 토양 수분을 확인한 점이 좋았어요.", List.of(
                        new RecordFeedbackNextActionResult("오후에 토양 표면이 마르는지 한 번 더 확인하세요.", RecordFeedbackActionDue.TODAY, RecordFeedbackActionCategory.IRRIGATION),
                        new RecordFeedbackNextActionResult("이번 주 안에 배수로 주변의 막힌 흙을 정리하세요.", RecordFeedbackActionDue.THIS_WEEK, RecordFeedbackActionCategory.CULTIVATION)))));
        mockMvc.perform(get("/api/v1/farming-records/{recordId}/feedback", recordId)
                .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", equalTo("READY")))
            .andExpect(jsonPath("$.data.feedback.goodPoint.text", equalTo("점적관수로 토양 수분을 확인한 점이 좋았어요.")))
            .andExpect(jsonPath("$.data.feedback.goodPoint.basis").doesNotExist())
            .andExpect(jsonPath("$.data.feedback.goodPoint.evidenceRefs").doesNotExist())
            .andExpect(jsonPath("$.data.feedback.nextActions[0].text", equalTo("오후에 토양 표면이 마르는지 한 번 더 확인하세요.")))
            .andExpect(jsonPath("$.data.feedback.nextActions[0].due", equalTo("TODAY")))
            .andExpect(jsonPath("$.data.feedback.nextActions[0].category", equalTo("IRRIGATION")))
            .andExpect(jsonPath("$.data.feedback.nextActions[0].basis").doesNotExist())
            .andExpect(jsonPath("$.data.feedback.nextActions[0].evidenceRefs").doesNotExist())
            .andExpect(jsonPath("$.data.feedback.citations").doesNotExist())
            .andExpect(jsonPath("$.data.feedback.inputSnapshot").doesNotExist())
            .andExpect(jsonPath("$.data.feedback.audit").doesNotExist())
            .andExpect(jsonPath("$.data.feedback.model").doesNotExist())
            .andExpect(jsonPath("$.data.citations").doesNotExist())
            .andExpect(jsonPath("$.data.auditStatus").doesNotExist())
            .andExpect(jsonPath("$.data.modelName").doesNotExist());
    }

    @Test
    @DisplayName("regenerate returns retried pending status")
    void regenerate_returns_retried_pending_status() throws Exception {
        when(queryService.regenerate(memberId, recordId)).thenReturn(result(RecordFeedbackStatus.PENDING, false, null, null));
        mockMvc.perform(post("/api/v1/farming-records/{recordId}/feedback/regenerate", recordId)
                .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", equalTo("PENDING")))
            .andExpect(jsonPath("$.data.failureCode").isEmpty());
    }

    @Test
    @DisplayName("get status without principal returns unauthorized")
    void get_status_without_principal_returns_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/farming-records/{recordId}/feedback", recordId))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code", equalTo("AUTH_001")));
    }

    @Test
    @DisplayName("regenerate with invalid principal returns unauthorized")
    void regenerate_with_invalid_principal_returns_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/farming-records/{recordId}/feedback/regenerate", recordId)
                .with(authenticatedMember("not-a-uuid")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code", equalTo("AUTH_001")));
    }

    @Test
    @DisplayName("get status propagates owned record not found")
    void get_status_propagates_owned_record_not_found() throws Exception {
        when(queryService.get(memberId, recordId)).thenThrow(new BusinessException(ErrorCode.FARMING_RECORD_NOT_FOUND));
        mockMvc.perform(get("/api/v1/farming-records/{recordId}/feedback", recordId)
                .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code", equalTo("FARMING_001")));
    }

    @Test
    @DisplayName("regenerate propagates conflict for a nonfailed or noncurrent feedback")
    void regenerate_propagates_conflict_for_a_nonfailed_or_noncurrent_feedback() throws Exception {
        when(queryService.regenerate(memberId, recordId)).thenThrow(new BusinessException(ErrorCode.RECORD_FEEDBACK_REGENERATION_NOT_ALLOWED));
        mockMvc.perform(post("/api/v1/farming-records/{recordId}/feedback/regenerate", recordId)
                .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code", equalTo("COACHING_002")));
    }

    private RequestPostProcessor authenticatedMember(String id) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    id, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            return request;
        };
    }

    private RecordFeedbackDetailResult result(RecordFeedbackStatus status, boolean inputPrepared,
                                              String failureCode, RecordFeedbackResultContent content) {
        return new RecordFeedbackDetailResult(feedbackId, recordId, status, 3, inputPrepared, failureCode, content,
                LocalDateTime.of(2026, 7, 11, 10, 0), LocalDateTime.of(2026, 7, 11, 10, 1));
    }

}
