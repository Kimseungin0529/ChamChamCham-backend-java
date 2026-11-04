package com.chamchamcham.application.report;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record FarmingCycleReportCursorPayload(
    @NonNull LocalDateTime endsAt,
    @NonNull UUID finalHarvestRecordId
) {
    public FarmingCycleReportCursorPayload {
        Objects.requireNonNull(endsAt, "endsAt");
        Objects.requireNonNull(finalHarvestRecordId, "finalHarvestRecordId");
    }

    @NonNull
    public LocalDateTime getEndsAt() { return endsAt; }
    @NonNull
    public UUID getFinalHarvestRecordId() { return finalHarvestRecordId; }
}
