package com.chamchamcham.application.policy.sync;

import com.chamchamcham.domain.policy.PolicySource;
import com.chamchamcham.domain.policy.PolicySyncJob;
import com.chamchamcham.domain.policy.PolicySyncJobStatus;
import com.chamchamcham.domain.policy.PolicySyncTriggerType;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class PolicySyncResult {
    private PolicySyncResult() {}

    public record JobSummary(
        @NonNull UUID jobId,
        @NonNull PolicySyncJobStatus status,
        @NonNull String targetYear
    ) {
        public JobSummary {
            Objects.requireNonNull(jobId, "jobId");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(targetYear, "targetYear");
        }

        @NonNull
        public UUID getJobId() { return jobId; }
        public static JobSummary from(PolicySyncJob job) {
            Objects.requireNonNull(job, "job");
            if (job.getId() == null) throw new IllegalArgumentException("Required value was null.");
            return new JobSummary(job.getId(), job.getStatus(), job.getTargetYear());
        }

        @NonNull
        public PolicySyncJobStatus getStatus() { return status; }
        @NonNull
        public String getTargetYear() { return targetYear; }
    }

    public record JobDetail(
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
        public JobDetail {
            Objects.requireNonNull(jobId, "jobId");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(targetYear, "targetYear");
            Objects.requireNonNull(triggerType, "triggerType");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(startedAt, "startedAt");
        }

        @NonNull
        public UUID getJobId() { return jobId; }
        public static JobDetail from(PolicySyncJob job) {
            Objects.requireNonNull(job, "job");
            if (job.getId() == null) throw new IllegalArgumentException("Required value was null.");
            return new JobDetail(job.getId(), job.getSource(), job.getTargetYear(), job.getTriggerType(), job.getStatus(), job.getTotalCount(), job.getSyncedCount(), job.getDetailSuccessCount(), job.getDetailFailureCount(), job.getErrorMessage(), job.getStartedAt(), job.getFinishedAt());
        }

        @NonNull
        public PolicySource getSource() { return source; }
        @NonNull
        public String getTargetYear() { return targetYear; }
        @NonNull
        public PolicySyncTriggerType getTriggerType() { return triggerType; }
        @NonNull
        public PolicySyncJobStatus getStatus() { return status; }
        public int getTotalCount() { return totalCount; }
        public int getSyncedCount() { return syncedCount; }
        public int getDetailSuccessCount() { return detailSuccessCount; }
        public int getDetailFailureCount() { return detailFailureCount; }
        @Nullable
        public String getErrorMessage() { return errorMessage; }
        @NonNull
        public LocalDateTime getStartedAt() { return startedAt; }
        @Nullable
        public LocalDateTime getFinishedAt() { return finishedAt; }
    }
}
