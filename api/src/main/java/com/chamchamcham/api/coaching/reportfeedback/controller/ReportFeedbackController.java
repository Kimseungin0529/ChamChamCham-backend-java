package com.chamchamcham.api.coaching.reportfeedback.controller;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import java.util.UUID;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.chamchamcham.api.coaching.reportfeedback.dto.ReportFeedbackResponses;
import com.chamchamcham.application.coaching.reportfeedback.lifecycle.ReportFeedbackQueryService;
import com.chamchamcham.domain.farming.WorkType;

@RestController
@RequestMapping("/api/v1/farming-reports/{reportId}/feedback")
public class ReportFeedbackController {
    private final ReportFeedbackQueryService queryService;

    public ReportFeedbackController(ReportFeedbackQueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ReportFeedbackResponses.ListResponse>> getStatus(
            @AuthenticationPrincipal String memberId, @PathVariable UUID reportId) {
        var result = queryService.get(parseMemberId(memberId), reportId);
        return ResponseEntity.ok(ApiResponse.ok(ReportFeedbackResponses.ListResponse.from(result)));
    }

    @PostMapping("/{workType}/regenerate")
    public ResponseEntity<ApiResponse<ReportFeedbackResponses.StatusResponse>> regenerate(
            @AuthenticationPrincipal String memberId, @PathVariable UUID reportId, @PathVariable WorkType workType) {
        var result = queryService.regenerate(parseMemberId(memberId), reportId, workType);
        return ResponseEntity.ok(ApiResponse.ok(ReportFeedbackResponses.StatusResponse.from(result)));
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
