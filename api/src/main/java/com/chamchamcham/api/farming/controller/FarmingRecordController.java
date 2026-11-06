package com.chamchamcham.api.farming.controller;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import com.chamchamcham.api.farming.dto.FarmingRecordRequests;
import com.chamchamcham.api.farming.dto.FarmingRecordResponses;
import com.chamchamcham.application.farming.FarmingRecordCommand;
import com.chamchamcham.application.farming.FarmingRecordSearchCondition;
import com.chamchamcham.application.farming.FarmingRecordService;
import com.chamchamcham.domain.farming.WorkType;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import kotlin.Unit;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/farming-records")
public class FarmingRecordController {
    private final FarmingRecordService farmingRecordService;
    public FarmingRecordController(FarmingRecordService farmingRecordService) { this.farmingRecordService = farmingRecordService; }
    @PostMapping
    public ResponseEntity<ApiResponse<FarmingRecordResponses.RecordIdResponse>> createRecord(@AuthenticationPrincipal String memberId,
        @Valid @RequestBody FarmingRecordRequests.SaveRecordRequest request) {
        var result = farmingRecordService.create(request.toCreateCommand(parseMemberId(memberId)));
        return ResponseEntity.ok(ApiResponse.ok(FarmingRecordResponses.RecordIdResponse.from(result)));
    }
    @GetMapping
    public ResponseEntity<ApiResponse<FarmingRecordResponses.RecordPageResponse>> listRecords(@AuthenticationPrincipal String memberId,
        @RequestParam(name = "cropIds", required = false) List<UUID> cropIds,
        @RequestParam(name = "workTypes", required = false) List<WorkType> workTypes,
        @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "cursor", required = false) String cursor,
        @RequestParam(name = "size", defaultValue = "20") int size) throws JsonProcessingException {
        var page = farmingRecordService.search(new FarmingRecordSearchCondition(parseMemberId(memberId),
            cropIds == null ? Collections.emptyList() : cropIds,
            workTypes == null ? Collections.emptyList() : workTypes,
            startDate,
            endDate,
            keyword,
            cursor,
            size));
        return ResponseEntity.ok(ApiResponse.ok(FarmingRecordResponses.RecordPageResponse.from(page)));
    }
    @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponse<FarmingRecordResponses.RecordDetailResponse>> getRecord(@AuthenticationPrincipal String memberId,
        @PathVariable("recordId") UUID recordId) {
        var detail = farmingRecordService.getDetail(parseMemberId(memberId), recordId);
        return ResponseEntity.ok(ApiResponse.ok(FarmingRecordResponses.RecordDetailResponse.from(detail)));
    }
    @PatchMapping("/{recordId}")
    public ResponseEntity<ApiResponse<FarmingRecordResponses.RecordIdResponse>> updateRecord(@AuthenticationPrincipal String memberId,
        @PathVariable("recordId") UUID recordId, @Valid @RequestBody FarmingRecordRequests.SaveRecordRequest request) {
        var result = farmingRecordService.update(request.toUpdateCommand(parseMemberId(memberId), recordId));
        return ResponseEntity.ok(ApiResponse.ok(FarmingRecordResponses.RecordIdResponse.from(result)));
    }
    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponse<Unit>> deleteRecord(@AuthenticationPrincipal String memberId, @PathVariable("recordId") UUID recordId) {
        farmingRecordService.delete(new FarmingRecordCommand.Delete(parseMemberId(memberId), recordId));
        return ResponseEntity.ok(ApiResponse.empty(Unit.INSTANCE));
    }

    private UUID parseMemberId(String memberId) {
        if (memberId == null || memberId.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try { return UUID.fromString(memberId); }
        catch (IllegalArgumentException exception) { throw new BusinessException(ErrorCode.UNAUTHORIZED); }
    }
}
