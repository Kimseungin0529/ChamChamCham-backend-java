package com.chamchamcham.application.coaching.recordfeedback.lifecycle;

import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackGenerationRequested(
    @NonNull UUID feedbackId,
    @NonNull UUID memberId,
    @NonNull UUID recordId,
     long sourceRevision
) {
    public RecordFeedbackGenerationRequested {
        Objects.requireNonNull(feedbackId, "feedbackId");
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(recordId, "recordId");
    }
    @NonNull public UUID getFeedbackId() { return feedbackId; }
    @NonNull public UUID getMemberId() { return memberId; }
    @NonNull public UUID getRecordId() { return recordId; }
    public long getSourceRevision() { return sourceRevision; }
}
