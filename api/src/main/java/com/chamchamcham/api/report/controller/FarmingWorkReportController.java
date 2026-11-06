package com.chamchamcham.api.report.controller;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import com.chamchamcham.api.report.dto.FarmingWorkReportResponses;
import com.chamchamcham.application.report.FarmingWorkReportQueryService;
import com.chamchamcham.application.report.FarmingWorkReportSearchCondition;
import com.chamchamcham.domain.farming.WorkType;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.validation.annotation.Validated;
import java.util.Set;
import java.util.List;
import java.util.Collections;

@RestController
@Validated
@RequestMapping("/api/v1/farming-reports")
public class FarmingWorkReportController {
    private final FarmingWorkReportQueryService queryService;
    public FarmingWorkReportController(FarmingWorkReportQueryService queryService) { this.queryService = queryService; }
    @GetMapping("/work-items")
    public ResponseEntity<ApiResponse<FarmingWorkReportResponses.PageResponse>> list(@AuthenticationPrincipal String memberId,
        @RequestParam(name = "farmId", required = false) Set<UUID> farmId,
        @RequestParam(name = "cropId", required = false) Set<UUID> cropId,
        @RequestParam(name = "workType", required = false) List<WorkType> workTypes,
        @RequestParam(name = "cursor", required = false) String cursor,
        @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) throws JsonProcessingException {
        final com.chamchamcham.application.report.FarmingWorkReportResult.Page result;
        try {
            result = queryService.list(new FarmingWorkReportSearchCondition(parseMemberId(memberId), farmId == null ? Collections.emptySet() : farmId,
                cropId == null ? Collections.emptySet() : cropId, workTypes == null ? Collections.emptyList() : workTypes, cursor, size));
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == ErrorCode.INVALID_CURSOR) throw new BusinessException(ErrorCode.INVALID_INPUT);
            throw exception;
        }
        return ResponseEntity.ok(ApiResponse.ok(FarmingWorkReportResponses.PageResponse.from(result)));
    }
    @GetMapping("/{reportId}/work-types/{workType}")
    public ResponseEntity<ApiResponse<FarmingWorkReportResponses.DetailResponse>> getDetail(@AuthenticationPrincipal String memberId,
        @PathVariable("reportId") UUID reportId, @PathVariable("workType") WorkType workType) {
        var result = queryService.getDetail(parseMemberId(memberId), reportId, workType);
        return ResponseEntity.ok(ApiResponse.ok(FarmingWorkReportResponses.DetailResponse.from(result)));
    }

    private UUID parseMemberId(String memberId) {
        if (memberId == null || memberId.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try { return UUID.fromString(memberId); }
        catch (IllegalArgumentException exception) { throw new BusinessException(ErrorCode.UNAUTHORIZED); }
    }
}
