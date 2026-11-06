package com.chamchamcham.api.policy.controller;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.api.policy.dto.sync.PolicySyncJobDetailResponse;
import com.chamchamcham.api.policy.dto.sync.PolicySyncJobSummaryResponse;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.policy.sync.PolicySyncAsyncRunner;
import com.chamchamcham.application.policy.sync.PolicySyncService;
import com.chamchamcham.domain.policy.PolicySyncJobStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/policies/sync-jobs")
public class AdminPolicySyncController {
    private final PolicySyncService policySyncService;
    private final PolicySyncAsyncRunner policySyncAsyncRunner;

    public AdminPolicySyncController(PolicySyncService policySyncService, PolicySyncAsyncRunner policySyncAsyncRunner) {
        this.policySyncService = java.util.Objects.requireNonNull(policySyncService, "policySyncService");
        this.policySyncAsyncRunner = java.util.Objects.requireNonNull(policySyncAsyncRunner, "policySyncAsyncRunner");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PolicySyncJobSummaryResponse>> createJob(
        @AuthenticationPrincipal @Nullable Object principal
    ) {
        var result = policySyncService.createAdminSyncJob(parseMemberId(principal));
        if (result.getStatus() == PolicySyncJobStatus.RUNNING) {
            policySyncAsyncRunner.run(result.getJobId());
        }
        return ResponseEntity.ok(ApiResponse.ok(PolicySyncJobSummaryResponse.from(result)));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<PolicySyncJobDetailResponse>> getJob(@PathVariable UUID jobId) {
        var result = policySyncService.getJob(jobId);
        return ResponseEntity.ok(ApiResponse.ok(PolicySyncJobDetailResponse.from(result)));
    }

    private UUID parseMemberId(@Nullable Object principal) {
        String memberId = principal instanceof String value ? value
            : principal instanceof UserDetails userDetails ? userDetails.getUsername() : null;
        if (memberId == null || memberId.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return UUID.fromString(memberId);
        } catch (Throwable exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
