package com.chamchamcham.api.farm.controller;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.api.farm.dto.FarmRequests;
import com.chamchamcham.api.farm.dto.FarmResponses;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.farm.FarmCommand;
import com.chamchamcham.application.farm.FarmService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/farms")
public class FarmController {
    private final FarmService farmService;
    public FarmController(FarmService farmService) { this.farmService = farmService; }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FarmResponses.FarmResponse>>> list(@AuthenticationPrincipal String memberId) {
        return ResponseEntity.ok(ApiResponse.ok(farmService.list(parseMemberId(memberId)).stream()
            .map(FarmResponses.FarmResponse::from).collect(Collectors.toList())));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<FarmResponses.FarmResponse>> create(@AuthenticationPrincipal String memberId,
            @Valid @RequestBody FarmRequests.SaveFarmRequest request) {
        var result = farmService.create(new FarmCommand.Create(parseMemberId(memberId), request.toCommandDraft(), request.cropIds()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(FarmResponses.FarmResponse.from(result)));
    }
    @PutMapping("/{farmId}")
    public ResponseEntity<ApiResponse<FarmResponses.FarmResponse>> replace(@AuthenticationPrincipal String memberId,
            @PathVariable UUID farmId, @Valid @RequestBody FarmRequests.SaveFarmRequest request) {
        var result = farmService.replace(new FarmCommand.Replace(parseMemberId(memberId), farmId, request.toCommandDraft(), request.cropIds()));
        return ResponseEntity.ok(ApiResponse.ok(FarmResponses.FarmResponse.from(result)));
    }
    @DeleteMapping("/{farmId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal String memberId, @PathVariable UUID farmId) {
        farmService.delete(new FarmCommand.Delete(parseMemberId(memberId), farmId));
        return ResponseEntity.noContent().build();
    }
    private UUID parseMemberId(String memberId) {
        if (memberId == null || memberId.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try { return UUID.fromString(memberId); }
        catch (IllegalArgumentException exception) { throw new BusinessException(ErrorCode.UNAUTHORIZED); }
    }
}
