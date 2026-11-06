package com.chamchamcham.api.voice.controller;

import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.application.farming.FarmingRecordCommand;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.application.voice.FarmOption;
import com.chamchamcham.application.voice.VoiceRecordCandidate;
import com.chamchamcham.application.voice.VoiceSessionCommand;
import com.chamchamcham.application.voice.VoiceSessionResult;
import com.chamchamcham.application.voice.VoiceSessionService;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.voice.VoiceSessionStatus;
import com.chamchamcham.domain.voice.VoiceTurnRole;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@WebMvcTest(VoiceSessionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class VoiceSessionControllerTest {
    @Autowired private MockMvc mockMvc;
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID farmId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private final UUID recordId = UUID.fromString("00000000-0000-0000-0000-000000000401");
    @MockBean private VoiceSessionService voiceSessionService;
    @MockBean private TokenProvider tokenProvider;
    @Test
    @DisplayName("create session returns ephemeral secret and farm options")
    void create_session_returns_ephemeral_secret_and_farm_options() throws Exception {
        when(voiceSessionService.create(new VoiceSessionCommand.Create(memberId)))
            .thenReturn(new VoiceSessionResult.Created(sessionId,
            "secret",
            LocalDateTime.of(2026,
            6,
            1,
            9,
            10),
            "gpt-realtime",
            java.util.Arrays.asList(new FarmOption(farmId,
        "약초농장")),
            java.util.Map.of(farmId.toString(),
            java.util.Collections.emptyList()),
            20,
            330));
        mockMvc.perform(post("/api/v1/voice-sessions").with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sessionId", equalTo(sessionId.toString())))
            .andExpect(jsonPath("$.data.clientSecret", equalTo("secret")))
            .andExpect(jsonPath("$.data.maxRounds", equalTo(20)))
            .andExpect(jsonPath("$.data.maxDurationSeconds", equalTo(330)));
    }

    @Test
    @DisplayName("create session without auth returns unauthorized")
    void create_session_without_auth_returns_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/voice-sessions"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code", equalTo("AUTH_001")));
    }

    @Test
    @DisplayName("submit turns maps request and returns missing fields")
    void submit_turns_maps_request_and_returns_missing_fields() throws Exception {
        when(voiceSessionService.submitTurns(new VoiceSessionCommand.SubmitTurns(memberId,
            sessionId,
            java.util.Arrays.asList(new VoiceSessionCommand.TurnInput(VoiceTurnRole.USER,
            "물 줬어요",
            null)),
            new VoiceRecordCandidate(null,
            null,
            WorkType.WATERING,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null)))).thenReturn(new VoiceSessionResult.Processed(sessionId,
            VoiceSessionStatus.WAITING_CONFIRMATION,
            new VoiceRecordCandidate(null,
            null,
            WorkType.WATERING,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null),
            java.util.Arrays.asList("farmId",
        "cropId")));
        mockMvc.perform(patch("/api/v1/voice-sessions/%s/turns".formatted(sessionId))
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {
                      "turns":[{"role":"USER","content":"물 줬어요"}],
                      "candidate":{"workType":"WATERING"}
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", equalTo("WAITING_CONFIRMATION")))
            .andExpect(jsonPath("$.data.missingFields[0]", equalTo("farmId")));
    }

    @Test
    @DisplayName("submit turns rejects more than 50 turns")
    void submit_turns_rejects_more_than_50_turns() throws Exception {
        var turns = java.util.stream.IntStream.rangeClosed(1, 51).mapToObj(it -> """
{"role":"USER","content":"발화 %s"}""".formatted(it)).collect(java.util.stream.Collectors.joining(","));
        mockMvc.perform(patch("/api/v1/voice-sessions/%s/turns".formatted(sessionId))
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
{"turns":[%s],"candidate":{"workType":"WATERING"}}""".formatted(turns)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("confirm forces entryMode to VOICE regardless of request body")
    void confirm_forces_entryMode_to_VOICE_regardless_of_request_body() throws Exception {
        when(voiceSessionService.confirm(expectedConfirmCommand()))
            .thenReturn(new VoiceSessionResult.Confirmed(sessionId, recordId, WorkType.WATERING));
        mockMvc.perform(post("/api/v1/voice-sessions/%s/confirm".formatted(sessionId))
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(confirmJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.recordId", equalTo(recordId.toString())));
    }

    @Test
    @DisplayName("cancel returns cancelled status")
    void cancel_returns_cancelled_status() throws Exception {
        when(voiceSessionService.cancel(new VoiceSessionCommand.Cancel(memberId, sessionId)))
            .thenReturn(new VoiceSessionResult.Cancelled(sessionId, VoiceSessionStatus.CANCELLED));
        mockMvc.perform(post("/api/v1/voice-sessions/%s/cancel".formatted(sessionId)).with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", equalTo("CANCELLED")));
    }

    @Test
    void turnElementsKeepOriginalNonCascadingValidationAndCandidateRemainsRequired() throws Exception {
        var candidate = new VoiceRecordCandidate();
        var command = new VoiceSessionCommand.SubmitTurns(memberId, sessionId,
            List.of(new VoiceSessionCommand.TurnInput(VoiceTurnRole.USER, " ", null)), candidate);
        when(voiceSessionService.submitTurns(command)).thenReturn(new VoiceSessionResult.Processed(
            sessionId, VoiceSessionStatus.WAITING_CONFIRMATION, candidate, List.of("farmId")));
        var json = "{\"turns\":[{\"role\":\"USER\",\"content\":\" \"}],\"candidate\":{}}";
        mockMvc.perform(patch("/api/v1/voice-sessions/{sessionId}/turns", sessionId)
            .with(authenticatedMember(memberId.toString())).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.missingFields[0]", equalTo("farmId")));
        mockMvc.perform(patch("/api/v1/voice-sessions/{sessionId}/turns", sessionId)
            .with(authenticatedMember(memberId.toString())).contentType(MediaType.APPLICATION_JSON)
            .content("{\"turns\":[{\"role\":\"USER\",\"content\":\"말\"}],\"candidate\":null}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code", equalTo("COMMON_002")));
    }


    private VoiceSessionCommand.Confirm expectedConfirmCommand() {
        return new VoiceSessionCommand.Confirm(memberId,
            sessionId,
            new FarmingRecordCommand.Create(memberId,
            farmId,
            cropId,
            WorkType.WATERING,
            LocalDateTime.of(2026,
            6,
            1,
            9,
            0),
            "맑음",
            20,
            "물을 충분히 주었고 토양 상태도 양호하여 특별한 이상 없이 관수 작업을 마쳤습니다",
            null,
            null,
            null,
            null,
            null,
            null,
            java.util.Collections.emptyList(),
            EntryMode.VOICE));
    }

    private String confirmJson() {
        return """
        {
          "farmId":"%s",
          "cropId":"%s",
          "workType":"WATERING",
          "workedAt":"2026-06-01T09:00:00",
          "weatherCondition":"맑음",
          "weatherTemperature":20,
          "memo":"물을 충분히 주었고 토양 상태도 양호하여 특별한 이상 없이 관수 작업을 마쳤습니다",
          "entryMode":"MANUAL"
        }
    """.formatted(farmId, cropId);
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
