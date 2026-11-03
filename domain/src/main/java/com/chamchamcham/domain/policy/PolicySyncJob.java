package com.chamchamcham.domain.policy;

import com.chamchamcham.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "policy_sync_job")
public class PolicySyncJob extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PolicySource source;

    @Column(name = "target_year", nullable = false, length = 4)
    private String targetYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 32)
    private PolicySyncTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PolicySyncJobStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "synced_count", nullable = false)
    private int syncedCount;

    @Column(name = "detail_success_count", nullable = false)
    private int detailSuccessCount;

    @Column(name = "detail_failure_count", nullable = false)
    private int detailFailureCount;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_by_member_id", columnDefinition = "uuid")
    private UUID createdByMemberId;

    protected PolicySyncJob() {
    }

    public PolicySyncJob(
        PolicySource source,
        String targetYear,
        PolicySyncTriggerType triggerType,
        @Nullable UUID createdByMemberId
    ) {
        this(null, source, targetYear, triggerType, PolicySyncJobStatus.RUNNING,
            LocalDateTime.now(), null, 0, 0, 0, 0, null, createdByMemberId);
    }

    public PolicySyncJob(
        @Nullable UUID id,
        PolicySource source,
        String targetYear,
        PolicySyncTriggerType triggerType,
        PolicySyncJobStatus status,
        LocalDateTime startedAt,
        @Nullable LocalDateTime finishedAt,
        int totalCount,
        int syncedCount,
        int detailSuccessCount,
        int detailFailureCount,
        @Nullable String errorMessage,
        @Nullable UUID createdByMemberId
    ) {
        this.id = id;
        this.source = Objects.requireNonNull(source, "source");
        this.targetYear = Objects.requireNonNull(targetYear, "targetYear");
        this.triggerType = Objects.requireNonNull(triggerType, "triggerType");
        this.status = Objects.requireNonNull(status, "status");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.finishedAt = finishedAt;
        this.totalCount = totalCount;
        this.syncedCount = syncedCount;
        this.detailSuccessCount = detailSuccessCount;
        this.detailFailureCount = detailFailureCount;
        this.errorMessage = errorMessage;
        this.createdByMemberId = createdByMemberId;
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public PolicySource getSource() {
        return source;
    }

    public String getTargetYear() {
        return targetYear;
    }

    public PolicySyncTriggerType getTriggerType() {
        return triggerType;
    }

    public PolicySyncJobStatus getStatus() {
        return status;
    }

    public void setStatus(PolicySyncJobStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    @Nullable
    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(@Nullable LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getSyncedCount() {
        return syncedCount;
    }

    public void setSyncedCount(int syncedCount) {
        this.syncedCount = syncedCount;
    }

    public int getDetailSuccessCount() {
        return detailSuccessCount;
    }

    public void setDetailSuccessCount(int detailSuccessCount) {
        this.detailSuccessCount = detailSuccessCount;
    }

    public int getDetailFailureCount() {
        return detailFailureCount;
    }

    public void setDetailFailureCount(int detailFailureCount) {
        this.detailFailureCount = detailFailureCount;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Nullable
    public UUID getCreatedByMemberId() {
        return createdByMemberId;
    }

    public void succeed(int totalCount, int syncedCount, int detailSuccessCount, int detailFailureCount) {
        this.status = PolicySyncJobStatus.SUCCEEDED;
        this.totalCount = totalCount;
        this.syncedCount = syncedCount;
        this.detailSuccessCount = detailSuccessCount;
        this.detailFailureCount = detailFailureCount;
        this.errorMessage = null;
        this.finishedAt = LocalDateTime.now();
    }

    public void fail(String message) {
        Objects.requireNonNull(message, "message");
        this.status = PolicySyncJobStatus.FAILED;
        this.errorMessage = message.substring(0, Math.min(message.length(), 1000));
        this.finishedAt = LocalDateTime.now();
    }
}
