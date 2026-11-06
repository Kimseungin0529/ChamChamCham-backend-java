package com.chamchamcham.api.coaching.chat.controller;

import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.chamchamcham.application.coaching.chat.*;
import com.chamchamcham.api.coaching.chat.dto.*;
import com.chamchamcham.application.coaching.common.*;
import java.security.Principal;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(CoachingRagController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CoachingRagControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TokenProvider tokenProvider;
    @MockBean private CoachingRagService coachingRagService;

    @Test
    @DisplayName("query returns RAG answer")
    void query_returns_RAG_answer() throws Exception {
        doReturn(ragResult()).when(coachingRagService).answer(any(CoachingRagCommand.class));
        mockMvc.perform(post("/api/v1/coaching/rag/query")
                .with(user("00000000-0000-0000-0000-000000000001"))
                .principal((Principal) () -> "00000000-0000-0000-0000-000000000001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"과습 위험이 있을까?\",\"topK\":6}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.result.summary", equalTo("요약")))
            .andExpect(jsonPath("$.data.result.riskLevel", equalTo("LOW")))
            .andExpect(jsonPath("$.data.audit.status", equalTo("PASS")))
            .andExpect(jsonPath("$.data.model.embedding", equalTo("bge-m3")))
            .andExpect(jsonPath("$.data.model.chat", equalTo("openclaw/agri-rag-coach")))
            .andExpect(jsonPath("$.data.result.observations[0].citationLabels[0]", equalTo("근거 1: 농업기술길잡이 007 약용작물")))
            .andExpect(jsonPath("$.data.result.recommendations[0].citationLabels[0]", equalTo("근거 1: 농업기술길잡이 007 약용작물")))
            .andExpect(jsonPath("$.data.result.nextActions[0].citationLabels[0]", equalTo("근거 1: 농업기술길잡이 007 약용작물")))
            .andExpect(jsonPath("$.data.result.citations[0].displayLabel", equalTo("근거 1")))
            .andExpect(jsonPath("$.data.savedFeedbackId").doesNotExist());
    }

    @Test
    @DisplayName("query contract omits unused mode work type and saved feedback fields")
    void query_contract_omits_unused_mode_work_type_and_saved_feedback_fields() throws Exception {
        assertThat(Arrays.stream(CoachingRagRequests.QueryRequest.class.getDeclaredFields()).map(java.lang.reflect.Field::getName))
                .doesNotContain("mode", "workTypeId");
        assertThat(Arrays.stream(CoachingRagResponses.QueryResponse.class.getDeclaredFields()).map(java.lang.reflect.Field::getName))
                .doesNotContain("savedFeedbackId");
    }

    @Test
    @DisplayName("query rejects blank question")
    void query_rejects_blank_question() throws Exception {
        mockMvc.perform(post("/api/v1/coaching/rag/query")
                .with(user("00000000-0000-0000-0000-000000000001"))
                .principal((Principal) () -> "00000000-0000-0000-0000-000000000001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
    }

    @Test
    void queryRejectsMissingNullAndInvalidBounds() throws Exception {
        for (String json : List.of("{}", "{\"question\":null}", "{\"question\":\"   \"}",
                "{\"question\":\"질문\",\"topK\":0}", "{\"question\":\"질문\",\"topK\":21}")) {
            mockMvc.perform(post("/api/v1/coaching/rag/query")
                    .principal((Principal) () -> "00000000-0000-0000-0000-000000000001")
                    .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code", equalTo(
                            json.equals("{}") || json.equals("{\"question\":null}") ? "COMMON_002" : "COMMON_001")));
        }
        verifyNoInteractions(coachingRagService);
    }

    @Test
    void queryPreservesOptionalNullsAndBoundsInCommand() throws Exception {
        doReturn(ragResult()).when(coachingRagService).answer(any(CoachingRagCommand.class));
        for (String topK : List.of("null", "1", "20")) {
            mockMvc.perform(post("/api/v1/coaching/rag/query")
                    .principal((Principal) () -> "00000000-0000-0000-0000-000000000001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"question\":\"질문\",\"farmId\":null,\"cropId\":null,\"recordId\":null,\"periodStart\":null,\"periodEnd\":null,\"topK\":" + topK + "}"))
                    .andExpect(status().isOk());
        }
        var commands = org.mockito.ArgumentCaptor.forClass(CoachingRagCommand.class);
        verify(coachingRagService, times(3)).answer(commands.capture());
        assertThat(commands.getAllValues()).extracting(CoachingRagCommand::getTopK).containsExactly(null, 1, 20);
        assertThat(commands.getAllValues()).allSatisfy(command -> {
            assertThat(command.getMemberId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
            assertThat(command.getQuestion()).isEqualTo("질문");
            assertThat(command.getFarmId()).isNull();
            assertThat(command.getCropId()).isNull();
            assertThat(command.getRecordId()).isNull();
            assertThat(command.getPeriodStart()).isNull();
            assertThat(command.getPeriodEnd()).isNull();
        });
    }

    @Test
    void citationLabelsPreserveOrderDuplicatesAndUnknownReferences() throws Exception {
        var source = ragResult().getResult();
        var result = new CoachingStructuredResult(source.getSummary(), source.getRiskLevel(), source.getConfidence(),
                List.of(new CoachingObservation("제목", "내용", List.of("same", "missing", "same"))), source.getDiagnosis(),
                source.getRecommendations(), source.getNextActions(), source.getFollowUpQuestions(),
                List.of(new CoachingCitationRef("same", "첫 문서", RagSourceType.TECH_DOCUMENT, null, null, null),
                        new CoachingCitationRef("same", "다음 문서", RagSourceType.TECH_DOCUMENT, null, null, null)),
                source.getRecordQuality(), source.getLimitations());
        doReturn(new CoachingRagResult(result, ragResult().getAudit(), ragResult().getModel()))
                .when(coachingRagService).answer(any(CoachingRagCommand.class));
        mockMvc.perform(post("/api/v1/coaching/rag/query")
                .principal((Principal) () -> "00000000-0000-0000-0000-000000000001")
                .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"질문\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result.observations[0].citationLabels", equalTo(List.of("근거 2: 다음 문서", "근거 확인 필요", "근거 2: 다음 문서"))))
                .andExpect(jsonPath("$.data.result.citations[0].displayLabel", equalTo("근거 1")))
                .andExpect(jsonPath("$.data.result.citations[1].displayLabel", equalTo("근거 2")));
    }

    private CoachingRagResult ragResult() {
        return new CoachingRagResult(new CoachingStructuredResult("요약", CoachingRiskLevel.LOW, 0.8,
                List.of(new CoachingObservation("관수", "토양 수분 확인", List.of("doc-1"))), "진단",
                List.of(new CoachingRecommendation(CoachingPriority.MEDIUM, "토양 확인", "공식문서 근거", null, List.of("doc-1"))),
                List.of(new CoachingNextAction(CoachingActionDue.TODAY, "기록 보완", List.of("doc-1"))), List.of(),
                List.of(new CoachingCitationRef("doc-1", "농업기술길잡이 007 약용작물", RagSourceType.TECH_DOCUMENT, null, null, null)),
                new CoachingRecordQuality(), List.of()), new RagAuditResult(RagAuditStatus.PASS, List.of(), List.of()),
                new RagModelInfo("bge-m3", "openclaw/agri-rag-coach"));
    }

}
