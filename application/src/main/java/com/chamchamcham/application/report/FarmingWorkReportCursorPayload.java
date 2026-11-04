package com.chamchamcham.application.report;

import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record FarmingWorkReportCursorPayload(
    int version,
    @Nullable LocalDate lastWorkedOn,
    @NonNull FarmingCycleReportStatus status,
    @NonNull LocalDateTime sortAt,
    @NonNull UUID reportId,
    @NonNull WorkType workType
) {
    public FarmingWorkReportCursorPayload {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(sortAt, "sortAt");
        Objects.requireNonNull(reportId, "reportId");
        Objects.requireNonNull(workType, "workType");
    }

    public int getVersion() { return version; }
    @Nullable
    public LocalDate getLastWorkedOn() { return lastWorkedOn; }
    @NonNull
    public FarmingCycleReportStatus getStatus() { return status; }
    @NonNull
    public LocalDateTime getSortAt() { return sortAt; }
    @NonNull
    public UUID getReportId() { return reportId; }
    @NonNull
    public WorkType getWorkType() { return workType; }
    public static final int CURRENT_VERSION = 2;
}
