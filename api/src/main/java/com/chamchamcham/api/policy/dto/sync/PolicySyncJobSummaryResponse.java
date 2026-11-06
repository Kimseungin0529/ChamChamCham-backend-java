package com.chamchamcham.api.policy.dto.sync;

import com.chamchamcham.application.policy.sync.PolicySyncResult;
import com.chamchamcham.domain.policy.PolicySyncJobStatus;
import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record PolicySyncJobSummaryResponse(
    @NonNull UUID jobId,
    @NonNull PolicySyncJobStatus status,
    @NonNull String targetYear
) {
    public PolicySyncJobSummaryResponse {
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(targetYear, "targetYear");
    }

    @NonNull public UUID getJobId() { return jobId; }
    @NonNull public PolicySyncJobStatus getStatus() { return status; }
    @NonNull public String getTargetYear() { return targetYear; }

    public static PolicySyncJobSummaryResponse from(PolicySyncResult.JobSummary result) {
        return new PolicySyncJobSummaryResponse(
            result.getJobId(),
            result.getStatus(),
            result.getTargetYear()
        );
    }
}
