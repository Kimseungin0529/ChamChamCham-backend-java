package com.chamchamcham.api.policy.dto.sync;

import com.chamchamcham.application.policy.sync.PolicySyncResult;
import com.chamchamcham.domain.policy.PolicySource;
import com.chamchamcham.domain.policy.PolicySyncJobStatus;
import com.chamchamcham.domain.policy.PolicySyncTriggerType;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record PolicySyncJobDetailResponse(
    @NonNull UUID jobId,
    @NonNull PolicySource source,
    @NonNull String targetYear,
    @NonNull PolicySyncTriggerType triggerType,
    @NonNull PolicySyncJobStatus status,
    int totalCount,
    int syncedCount,
    int detailSuccessCount,
    int detailFailureCount,
    @Nullable String errorMessage,
    @NonNull LocalDateTime startedAt,
    @Nullable LocalDateTime finishedAt
) {
    public PolicySyncJobDetailResponse {
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(targetYear, "targetYear");
        Objects.requireNonNull(triggerType, "triggerType");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(startedAt, "startedAt");
    }

    @NonNull public UUID getJobId() { return jobId; }
    @NonNull public PolicySource getSource() { return source; }
    @NonNull public String getTargetYear() { return targetYear; }
    @NonNull public PolicySyncTriggerType getTriggerType() { return triggerType; }
    @NonNull public PolicySyncJobStatus getStatus() { return status; }
    public int getTotalCount() { return totalCount; }
    public int getSyncedCount() { return syncedCount; }
    public int getDetailSuccessCount() { return detailSuccessCount; }
    public int getDetailFailureCount() { return detailFailureCount; }
    @Nullable public String getErrorMessage() { return errorMessage; }
    @NonNull public LocalDateTime getStartedAt() { return startedAt; }
    @Nullable public LocalDateTime getFinishedAt() { return finishedAt; }

    public static PolicySyncJobDetailResponse from(PolicySyncResult.JobDetail result) {
        return new PolicySyncJobDetailResponse(
            result.getJobId(),
            result.getSource(),
            result.getTargetYear(),
            result.getTriggerType(),
            result.getStatus(),
            result.getTotalCount(),
            result.getSyncedCount(),
            result.getDetailSuccessCount(),
            result.getDetailFailureCount(),
            result.getErrorMessage(),
            result.getStartedAt(),
            result.getFinishedAt()
        );
    }
}
