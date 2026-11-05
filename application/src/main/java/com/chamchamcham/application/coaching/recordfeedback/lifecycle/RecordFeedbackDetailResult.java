package com.chamchamcham.application.coaching.recordfeedback.lifecycle;

import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionDue;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackDetailResult(
    @NonNull UUID feedbackId,
    @NonNull UUID recordId,
    @NonNull RecordFeedbackStatus status,
     long sourceRevision,
     boolean inputPrepared,
    @Nullable String failureCode,
    @Nullable RecordFeedbackResultContent content,
    @NonNull LocalDateTime createdAt,
    @NonNull LocalDateTime updatedAt
) {
    public RecordFeedbackDetailResult {
        Objects.requireNonNull(feedbackId, "feedbackId");
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
    @NonNull public UUID getFeedbackId() { return feedbackId; }
    @NonNull public UUID getRecordId() { return recordId; }
    @NonNull public RecordFeedbackStatus getStatus() { return status; }
    public long getSourceRevision() { return sourceRevision; }
    public boolean getInputPrepared() { return inputPrepared; }
    @Nullable public String getFailureCode() { return failureCode; }
    @Nullable public RecordFeedbackResultContent getContent() { return content; }
    @NonNull public LocalDateTime getCreatedAt() { return createdAt; }
    @NonNull public LocalDateTime getUpdatedAt() { return updatedAt; }
}
