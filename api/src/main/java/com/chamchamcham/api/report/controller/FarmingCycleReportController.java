package com.chamchamcham.api.report.controller;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import com.chamchamcham.api.report.dto.FarmingCycleReportResponses;
import com.chamchamcham.application.report.FarmingCycleReportQueryService;
import com.chamchamcham.application.report.FarmingCycleReportSearchCondition;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.validation.annotation.Validated;
import java.util.Set;
import java.util.Collections;

@RestController
@Validated
@RequestMapping("/api/v1/farming-reports")
public class FarmingCycleReportController {
    private final FarmingCycleReportQueryService queryService;
    public FarmingCycleReportController(FarmingCycleReportQueryService queryService) { this.queryService = queryService; }
    @GetMapping
    public ResponseEntity<ApiResponse<FarmingCycleReportResponses.PageResponse>> listCompleted(@AuthenticationPrincipal String memberId,
        @RequestParam(name = "farmId", required = false) Set<UUID> farmId,
        @RequestParam(name = "cropId", required = false) Set<UUID> cropId,
        @RequestParam(name = "cursor", required = false) String cursor,
        @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) throws JsonProcessingException {
        final com.chamchamcham.application.report.FarmingCycleReportResult.Page result;
        try {
            result = queryService.listCompleted(new FarmingCycleReportSearchCondition(parseMemberId(memberId),
                farmId == null ? Collections.emptySet() : farmId,
                cropId == null ? Collections.emptySet() : cropId, cursor, size));
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == ErrorCode.INVALID_CURSOR) throw new BusinessException(ErrorCode.INVALID_INPUT);
            throw exception;
        }
        return ResponseEntity.ok(ApiResponse.ok(FarmingCycleReportResponses.PageResponse.from(result)));
    }
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<FarmingCycleReportResponses.DetailResponse>> getDetail(@AuthenticationPrincipal String memberId,
        @PathVariable("reportId") UUID reportId) {
        var result = queryService.getDetail(parseMemberId(memberId), reportId);
        return ResponseEntity.ok(ApiResponse.ok(FarmingCycleReportResponses.DetailResponse.from(result)));
    }

    private UUID parseMemberId(String memberId) {
        if (memberId == null || memberId.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try { return UUID.fromString(memberId); }
        catch (IllegalArgumentException exception) { throw new BusinessException(ErrorCode.UNAUTHORIZED); }
    }
}
