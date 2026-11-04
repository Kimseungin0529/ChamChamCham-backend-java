package com.chamchamcham.application.report;

import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record ReportScope(
    @NonNull UUID memberId,
    @NonNull UUID farmId,
    @NonNull UUID cropId
) {
    public ReportScope {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(farmId, "farmId");
        Objects.requireNonNull(cropId, "cropId");
    }

    @NonNull
    public UUID getMemberId() { return memberId; }
    @NonNull
    public UUID getFarmId() { return farmId; }
    @NonNull
    public UUID getCropId() { return cropId; }
}
