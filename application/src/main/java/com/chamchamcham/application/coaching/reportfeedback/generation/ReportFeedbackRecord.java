package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.farming.WorkType;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record ReportFeedbackRecord(
    @NonNull UUID id,
    @NonNull LocalDateTime workedAt,
    @NonNull WorkType workType,
    @NonNull String memo,
    @NonNull Map<String, Object> details
) {
    public ReportFeedbackRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(workedAt, "workedAt");
        Objects.requireNonNull(workType, "workType");
        Objects.requireNonNull(memo, "memo");
        Objects.requireNonNull(details, "details");
    }
    @NonNull public UUID getId() { return id; }
    @NonNull public LocalDateTime getWorkedAt() { return workedAt; }
    @NonNull public WorkType getWorkType() { return workType; }
    @NonNull public String getMemo() { return memo; }
    @NonNull public Map<String, Object> getDetails() { return details; }
}
