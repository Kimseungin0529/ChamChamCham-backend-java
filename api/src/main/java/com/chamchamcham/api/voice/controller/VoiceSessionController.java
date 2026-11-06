package com.chamchamcham.api.voice.controller;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import com.chamchamcham.api.farming.dto.FarmingRecordRequests;
import com.chamchamcham.api.voice.dto.VoiceSessionRequests;
import com.chamchamcham.api.voice.dto.VoiceSessionResponses;
import com.chamchamcham.application.voice.VoiceSessionCommand;
import com.chamchamcham.application.voice.VoiceSessionService;
import com.chamchamcham.domain.farming.EntryMode;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/voice-sessions")
public class VoiceSessionController {
    private final VoiceSessionService voiceSessionService;
    public VoiceSessionController(VoiceSessionService voiceSessionService) { this.voiceSessionService = voiceSessionService; }
    @PostMapping
    public ResponseEntity<ApiResponse<VoiceSessionResponses.CreatedResponse>> createSession(@AuthenticationPrincipal String memberId) {
        var result = voiceSessionService.create(new VoiceSessionCommand.Create(parseMemberId(memberId)));
        return ResponseEntity.ok(ApiResponse.ok(VoiceSessionResponses.CreatedResponse.from(result)));
    }
    @PatchMapping("/{sessionId}/turns")
    public ResponseEntity<ApiResponse<VoiceSessionResponses.ProcessedResponse>> submitTurns(@AuthenticationPrincipal String memberId,
        @PathVariable("sessionId") UUID sessionId, @Valid @RequestBody VoiceSessionRequests.SubmitTurnsRequest request) {
        var result = voiceSessionService.submitTurns(request.toCommand(parseMemberId(memberId), sessionId));
        return ResponseEntity.ok(ApiResponse.ok(VoiceSessionResponses.ProcessedResponse.from(result)));
    }
    @PostMapping("/{sessionId}/confirm")
    public ResponseEntity<ApiResponse<VoiceSessionResponses.ConfirmedResponse>> confirm(@AuthenticationPrincipal String memberId,
        @PathVariable("sessionId") UUID sessionId, @Valid @RequestBody FarmingRecordRequests.SaveRecordRequest request) {
        var parsedMemberId = parseMemberId(memberId);
        var result = voiceSessionService.confirm(new VoiceSessionCommand.Confirm(parsedMemberId,
            sessionId,
            request.toCreateCommand(parsedMemberId,
            EntryMode.VOICE)));
        return ResponseEntity.ok(ApiResponse.ok(VoiceSessionResponses.ConfirmedResponse.from(result)));
    }
    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<ApiResponse<VoiceSessionResponses.CancelledResponse>> cancel(@AuthenticationPrincipal String memberId,
        @PathVariable("sessionId") UUID sessionId) {
        var result = voiceSessionService.cancel(new VoiceSessionCommand.Cancel(parseMemberId(memberId), sessionId));
        return ResponseEntity.ok(ApiResponse.ok(VoiceSessionResponses.CancelledResponse.from(result)));
    }

    private UUID parseMemberId(String memberId) {
        if (memberId == null || memberId.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try { return UUID.fromString(memberId); }
        catch (IllegalArgumentException exception) { throw new BusinessException(ErrorCode.UNAUTHORIZED); }
    }
}
