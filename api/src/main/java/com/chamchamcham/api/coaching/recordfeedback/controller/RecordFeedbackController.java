package com.chamchamcham.api.coaching.recordfeedback.controller;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import java.util.UUID;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.chamchamcham.api.coaching.recordfeedback.dto.RecordFeedbackResponses;
import com.chamchamcham.application.coaching.recordfeedback.lifecycle.RecordFeedbackQueryService;

@RestController
@RequestMapping("/api/v1/farming-records/{recordId}/feedback")
public class RecordFeedbackController {
    private final RecordFeedbackQueryService queryService;

    public RecordFeedbackController(RecordFeedbackQueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<RecordFeedbackResponses.StatusResponse>> getStatus(
            @AuthenticationPrincipal String memberId, @PathVariable UUID recordId) {
        var result = queryService.get(parseMemberId(memberId), recordId);
        return ResponseEntity.ok(ApiResponse.ok(RecordFeedbackResponses.StatusResponse.from(result)));
    }

    @PostMapping("/regenerate")
    public ResponseEntity<ApiResponse<RecordFeedbackResponses.StatusResponse>> regenerate(
            @AuthenticationPrincipal String memberId, @PathVariable UUID recordId) {
        var result = queryService.regenerate(parseMemberId(memberId), recordId);
        return ResponseEntity.ok(ApiResponse.ok(RecordFeedbackResponses.StatusResponse.from(result)));
    }

    private UUID parseMemberId(String memberId) {
        if (memberId == null || memberId.codePoints().allMatch(value -> Character.isWhitespace(value) || Character.isSpaceChar(value))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return UUID.fromString(memberId);
        } catch (IllegalArgumentException ignored) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
